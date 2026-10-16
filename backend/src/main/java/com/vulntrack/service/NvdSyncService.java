package com.vulntrack.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vulntrack.model.LocalCve;
import com.vulntrack.model.NvdSyncState;
import com.vulntrack.repository.LocalCveRepository;
import com.vulntrack.repository.NvdSyncStateRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
@Slf4j
public class NvdSyncService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final LocalCveRepository localCveRepository;
    private final NvdSyncStateRepository syncStateRepository;

    @Value("${nvd.api.url:https://services.nvd.nist.gov/rest/json/cves/2.0}")
    private String nvdApiUrl;

    /**
     * Optional NVD API key (env NVD_API_KEY). Without one NVD allows 5
     * requests / 30 s; with a (free) key it allows 50 / 30 s. Paging is
     * throttled accordingly below.
     */
    @Value("${nvd.api.key:}")
    private String nvdApiKey = "";

    private static final int PAGE_SIZE = 1000;
    /** Delay between pages without a key: 5 req/30 s -> one page per ~6.5 s. */
    private static final long PAGE_DELAY_NO_KEY_MS = 6500L;
    /** Delay between pages with a key: 50 req/30 s -> comfortable margin. */
    private static final long PAGE_DELAY_WITH_KEY_MS = 1000L;    /** Backoff when NVD rate-limits us (403 without key / 429 with key). */
    private static final long RATE_LIMIT_BACKOFF_MS = 35000L;
    private static final int MAX_RATE_LIMIT_RETRIES = 3;

    private static final DateTimeFormatter NVD_DATE_FORMAT = DateTimeFormatter
        .ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");

    /**
     * Guards overlapping syncs (scheduled vs. manually triggered). A full NVD
     * sync pages through ~250k CVEs, so concurrent runs would duplicate work
     * and rows (cve_id is not unique).
     */
    private final AtomicBoolean syncRunning = new AtomicBoolean(false);

    /**
     * NVD throttles/bans anonymous-looking datacenter clients aggressively.
     * Identify ourselves on every call (NVD etiquette + fewer 403s). The API
     * key MUST travel as a header: NVD 2.0 answers 404 to ?apiKey=, valid or
     * not.
     */
    @PostConstruct
    public void initRestTemplate() {
        restTemplate.getInterceptors().add((request, body, execution) -> {
            request.getHeaders().set(
                "User-Agent",
                "VulnTrack/1.0 (local NVD cache sync; admin-triggered)"
            );
            if (hasApiKey()) {
                request.getHeaders().set("apiKey", nvdApiKey);
            }
            return execution.execute(request, body);
        });
    }

    /** True while a full or incremental sync is in progress. */
    public boolean isSyncRunning() {
        return syncRunning.get();
    }

    /** Snapshot of the persisted sync position for the status endpoint. */
    public NvdSyncState getSyncState() {
        return syncStateRepository.findById(1).orElse(null);
    }

    /**
     * Fire-and-forget full sync for the admin trigger endpoint. Resumes from
     * the last saved page (or from {@code from} when given) instead of
     * starting over. Returns false when a sync is already running. The
     * download outlasts any HTTP timeout, so it always runs on a background
     * thread.
     */
    public boolean tryStartFullSync(Integer from) {
        if (!syncRunning.compareAndSet(false, true)) {
            return false;
        }
        int resumeFrom = (from != null && from >= 0) ? from : loadResumeIndex();
        CompletableFuture.runAsync(() -> {
            try {
                doFullSyncFrom(resumeFrom);
            } catch (Exception e) {
                log.error("Manual NVD full sync failed", e);
            } finally {
                syncRunning.set(false);
            }
        });
        return true;
    }

    public boolean tryStartFullSync() {
        return tryStartFullSync(null);
    }

    /**
     * Crash recovery: if a previous run died mid-sync (deploy, recycle),
     * its last state row is still RUNNING — resume it on boot instead of
     * losing the progress.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void resumeInterruptedSync() {
        NvdSyncState state = getSyncState();
        if (state != null && "RUNNING".equals(state.getStatus())) {
            log.info(
                "Resuming interrupted NVD sync from startIndex {}",
                state.getLastStartIndex()
            );
            tryStartFullSync();
        }
    }

    private int loadResumeIndex() {
        NvdSyncState state = getSyncState();
        if (state != null && state.getLastStartIndex() != null
            && state.getLastStartIndex() > 0) {
            log.info("Resuming NVD sync from saved startIndex {}", state.getLastStartIndex());
            return state.getLastStartIndex();
        }
        return 0;
    }

    /**
     * Trigger initial sync on application startup
     */
    // @PostConstruct
    // @EventListener(ApplicationReadyEvent.class)
    public void initialize() {
        log.info("Checking if initial NVD sync is needed...");

        // Check if we have any data
        long count = localCveRepository.count();

        if (count == 0) {
            log.info("No local CVE data found. Starting initial full sync...");
            fullSync();
        } else {
            log.info("Found {} existing CVEs. Running incremental sync...", count);
            incrementalSync();
        }
    }

    /**
     * Full sync - run weekly on Sunday at 2 AM
     */
    @Scheduled(cron = "0 0 2 * * SUN")
    @Transactional
    public void fullSync() {
        if (!syncRunning.compareAndSet(false, true)) {
            log.info("NVD sync already running; skipping scheduled full sync");
            return;
        }
        try {
            doFullSyncFrom(0);
        } finally {
            syncRunning.set(false);
        }
    }

    private void doFullSyncFrom(int fromIndex) {
        log.info("Starting full NVD sync from startIndex {}", fromIndex);
        syncCves(null, fromIndex, true); // null means fetch all
        log.info("Completed full NVD sync");
    }

    /**
     * Incremental sync - run daily at 2 AM
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void incrementalSync() {
        if (!syncRunning.compareAndSet(false, true)) {
            log.info("NVD sync already running; skipping scheduled incremental sync");
            return;
        }
        try {
            log.info("Starting incremental NVD sync");

            LocalDateTime lastModified = localCveRepository.findLastModifiedDate()
                .orElse(LocalDateTime.now().minusDays(1));

            // Add 1 second to avoid duplicates
            syncCves(lastModified.plusSeconds(3), 0, false);

            log.info("Completed incremental NVD sync");
        } finally {
            syncRunning.set(false);
        }
    }

    /** Persist the next page to fetch so a crash/recycle can resume. */
    private void saveProgress(int nextStartIndex, Integer totalResults, String status) {
        saveProgress(nextStartIndex, totalResults, status, null);
    }

    /** Same, additionally recording why a run stopped. */
    private void saveProgress(
        int nextStartIndex, Integer totalResults, String status, String error
    ) {
        try {
            NvdSyncState state = getSyncState();
            if (state == null) {
                state = NvdSyncState.builder().id(1).build();
            }
            state.setLastStartIndex(nextStartIndex);
            state.setTotalResults(totalResults);
            state.setStatus(status);
            state.setLastError(error);
            state.setUpdatedAt(LocalDateTime.now());
            syncStateRepository.save(state);
        } catch (Exception e) {
            log.warn("Could not persist NVD sync progress: {}", e.getMessage());
        }
    }

    private void syncCves(LocalDateTime lastModStartDate, int fromIndex, boolean trackProgress) {
        int startIndex = fromIndex;
        int totalResults = 0;
        int rateLimitRetries = 0;
        long pageDelayMs = hasApiKey() ? PAGE_DELAY_WITH_KEY_MS : PAGE_DELAY_NO_KEY_MS;

        if (trackProgress) {
            saveProgress(startIndex, null, "RUNNING");
        }

        String abortError = null;

        do {
            try {
                String url = buildUrl(lastModStartDate, startIndex);
                log.info("Fetching CVEs from: {}", url);

                String response = restTemplate.getForObject(url, String.class);
                JsonNode root = objectMapper.readTree(response);

                totalResults = root.has("totalResults") ? root.get("totalResults").asInt() : 0;
                JsonNode vulnerabilities = root.get("vulnerabilities");

                if (vulnerabilities != null && vulnerabilities.isArray()) {
                    List<LocalCve> cves = new ArrayList<>();
                    for (JsonNode vulnNode : vulnerabilities) {
                        JsonNode cveNode = vulnNode.get("cve");
                        if (cveNode != null) {
                            processCveNode(cveNode, cves, startIndex);
                        }
                    }

                    // Bulk save
                    localCveRepository.saveAll(cves);
                    log.info(
                        "Saved {} CVEs (startIndex: {}, totalResults: {})",
                        cves.size(), startIndex, totalResults
                    );

                    // save to file

                }

                startIndex += PAGE_SIZE; // Results per page
                rateLimitRetries = 0;
                if (trackProgress) {
                    saveProgress(startIndex, totalResults, "RUNNING");
                }
                sleepQuietly(pageDelayMs);

            } catch (HttpClientErrorException.TooManyRequests e) {
                // NVD rate limit with an API key.
                if (++rateLimitRetries > MAX_RATE_LIMIT_RETRIES) {
                    abortError = "Aborted: NVD rate limit (429) hit repeatedly at startIndex "
                        + startIndex;
                    log.error(abortError);
                    break;
                }
                log.warn(
                    "NVD rate limit (429); backing off {} ms before retrying startIndex {}",
                    RATE_LIMIT_BACKOFF_MS, startIndex
                );
                sleepQuietly(RATE_LIMIT_BACKOFF_MS);
            } catch (HttpClientErrorException.Forbidden e) {
                // NVD answers 403 when the keyless quota is exceeded.
                if (++rateLimitRetries > MAX_RATE_LIMIT_RETRIES) {
                    abortError = "Aborted: NVD quota exceeded (403) repeatedly at startIndex "
                        + startIndex + " — datacenter IPs may need an API key (NVD_API_KEY is "
                        + (hasApiKey() ? "set" : "NOT set") + ")";
                    log.error(abortError);
                    break;
                }
                log.warn(
                    "NVD quota exceeded (403); backing off {} ms before retrying startIndex {}",
                    RATE_LIMIT_BACKOFF_MS, startIndex
                );
                sleepQuietly(RATE_LIMIT_BACKOFF_MS);
            } catch (Exception e) {
                abortError = "Aborted at startIndex " + startIndex + ": "
                    + e.getClass().getSimpleName() + ": " + e.getMessage();
                log.error("Error syncing CVEs at startIndex {}: {}", startIndex, e.getMessage());
                break;
            }
        } while (startIndex < totalResults);

        if (trackProgress) {
            if (abortError != null) {
                // Stay resumable: keep RUNNING + reason so the next trigger
                // (or boot recovery) continues from the saved page.
                saveProgress(startIndex, totalResults, "RUNNING", abortError);
            } else {
                saveProgress(startIndex, totalResults, "IDLE");
            }
        }
    }

    private boolean hasApiKey() {
        return nvdApiKey != null && !nvdApiKey.isBlank();
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("NVD sync interrupted", e);
        }
    }

    private void processCveNode(JsonNode cveNode, List<LocalCve> cves, int startIndex) {
        try {
            String cveId = cveNode.get("id").asText();

            // Check if CVE already exists in the current batch or database
            boolean alreadyExists = cves.stream().anyMatch(c -> c.getCveId().equals(cveId)) ||
                localCveRepository.findByCveId(cveId).isPresent();

            if (alreadyExists) {
                log.debug("Skipping duplicate CVE: {}", cveId);
                return;
            }

            JsonNode configurations = cveNode.get("configurations");
            if (configurations == null)
                return;

            // Extract CPE and version information
            for (JsonNode config : configurations) {
                JsonNode nodes = config.get("nodes");
                if (nodes == null)
                    continue;

                for (JsonNode node : nodes) {
                    JsonNode cpeMatches = node.get("cpeMatch");
                    if (cpeMatches == null)
                        continue;

                    for (JsonNode match : cpeMatches) {
                        boolean vulnerable = match.has("vulnerable")
                            && match.get("vulnerable").asBoolean();
                        if (!vulnerable)
                            continue;

                        String criteria = match.get("criteria").asText();
                        String[] cpeParts = criteria.split(":");

                        if (cpeParts.length >= 5) {
                            String vendor = cpeParts[3].toLowerCase();
                            String product = cpeParts[4].toLowerCase();
                            String vsi = match.has("versionStartIncluding")
                                ? match.get("versionStartIncluding").asText() : null;
                            String vse = match.has("versionStartExcluding")
                                ? match.get("versionStartExcluding").asText() : null;
                            String vei = match.has("versionEndIncluding")
                                ? match.get("versionEndIncluding").asText() : null;
                            String vee = match.has("versionEndExcluding")
                                ? match.get("versionEndExcluding").asText() : null;

                            // One CVE node can list the same vendor/product (with
                            // identical bounds) under several configuration
                            // nodes. The cveId check above runs once per node,
                            // so dedupe on the full identity here — otherwise
                            // twin rows hit the unique index and fail the
                            // whole page batch.
                            boolean twinInBatch = cves.stream().anyMatch(existing ->
                                existing.getCveId().equals(cveId)
                                    && Objects.equals(existing.getCpeVendor(), vendor)
                                    && Objects.equals(existing.getCpeProduct(), product)
                                    && Objects.equals(existing.getVersionStartIncluding(), vsi)
                                    && Objects.equals(existing.getVersionStartExcluding(), vse)
                                    && Objects.equals(existing.getVersionEndIncluding(), vei)
                                    && Objects.equals(existing.getVersionEndExcluding(), vee));
                            if (twinInBatch) {
                                log.debug(
                                    "Skipping duplicate identity {}:{} for {}",
                                    vendor, product, cveId
                                );
                                continue;
                            }

                            LocalCve localCve = LocalCve.builder()
                                .cveId(cveId)
                                .cpeProduct(product)
                                .cpeVendor(vendor)
                                .description(extractDescription(cveNode))
                                .cvssScore(extractCvssScore(cveNode))
                                .severity(extractSeverity(cveNode))
                                .vectorString(extractVectorString(cveNode))
                                .versionStartIncluding(vsi)
                                .versionStartExcluding(vse)
                                .versionEndIncluding(vei)
                                .versionEndExcluding(vee)
                                .publishedDate(parseDate(cveNode, "published"))
                                .lastModified(parseDate(cveNode, "lastModified"))
                                .syncedAt(LocalDateTime.now())
                                .build();

                            cves.add(localCve);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error processing CVE node: {}", e.getMessage());
        }
    }

    private String buildUrl(LocalDateTime lastModStartDate, int startIndex) {
        var builder = UriComponentsBuilder.fromHttpUrl(nvdApiUrl)
            .queryParam("resultsPerPage", PAGE_SIZE)
            .queryParam("startIndex", startIndex);

        // NOTE: the API key is sent as a header (see initRestTemplate), never
        // as ?apiKey= — NVD 2.0 rejects the query form with 404.

        if (lastModStartDate != null) {
            builder.queryParam(
                "lastModStartDate",
                lastModStartDate.format(NVD_DATE_FORMAT) + "Z"
            );
        }

        return builder.build().toUriString();
    }

    private LocalDateTime parseDate(JsonNode cveNode, String field) {
        try {
            if (cveNode.has(field)) {
                return LocalDateTime.parse(cveNode.get(field).asText(), NVD_DATE_FORMAT);
            }
        } catch (Exception e) {
            log.debug("Could not parse date for field: {}", field);
        }
        return null;
    }

    private String extractDescription(JsonNode cveNode) {
        JsonNode descriptions = cveNode.get("descriptions");
        if (descriptions != null && descriptions.isArray()) {
            for (JsonNode desc : descriptions) {
                if ("en".equals(desc.get("lang").asText())) {
                    String value = desc.get("value").asText();
                    return value.length() > 500 ? value.substring(0, 500) + "..." : value;
                }
            }
        }
        return "No description available";
    }

    private Double extractCvssScore(JsonNode cveNode) {
        try {
            JsonNode metrics = cveNode.get("metrics");
            if (metrics != null) {
                JsonNode cvssV31 = metrics.get("cvssMetricV31");
                if (cvssV31 != null && cvssV31.isArray() && cvssV31.size() > 0) {
                    return cvssV31.get(0).get("cvssData").get("baseScore").asDouble();
                }
            }
        } catch (Exception e) {
            log.debug("Could not extract CVSS score");
        }
        return null;
    }

    private String extractSeverity(JsonNode cveNode) {
        try {
            JsonNode metrics = cveNode.get("metrics");
            if (metrics != null) {
                JsonNode cvssV31 = metrics.get("cvssMetricV31");
                if (cvssV31 != null && cvssV31.isArray() && cvssV31.size() > 0) {
                    JsonNode cvssData = cvssV31.get(0).get("cvssData");
                    if (cvssData != null && cvssData.has("baseSeverity")) {
                        return cvssData.get("baseSeverity").asText();
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Could not extract severity");
        }
        return "UNKNOWN";
    }

    private String extractVectorString(JsonNode cveNode) {
        try {
            JsonNode metrics = cveNode.get("metrics");
            if (metrics != null) {
                JsonNode cvssV31 = metrics.get("cvssMetricV31");
                if (cvssV31 != null && cvssV31.isArray() && cvssV31.size() > 0) {
                    return cvssV31.get(0).get("cvssData").get("vectorString").asText();
                }
            }
        } catch (Exception e) {
            log.debug("Could not extract vector string");
        }
        return null;
    }
}