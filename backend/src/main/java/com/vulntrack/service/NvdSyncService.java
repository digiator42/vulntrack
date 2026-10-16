package com.vulntrack.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vulntrack.model.LocalCve;
import com.vulntrack.repository.LocalCveRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
@Slf4j
public class NvdSyncService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final LocalCveRepository localCveRepository;

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
    private static final long PAGE_DELAY_WITH_KEY_MS = 1000L;
    /** Backoff when NVD rate-limits us (403 without key / 429 with key). */
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

    /** True while a full or incremental sync is in progress. */
    public boolean isSyncRunning() {
        return syncRunning.get();
    }

    /**
     * Fire-and-forget full sync for the admin trigger endpoint. Returns false
     * when a sync is already running. The download outlasts any HTTP
     * timeout, so it always runs on a background thread.
     */
    public boolean tryStartFullSync() {
        if (!syncRunning.compareAndSet(false, true)) {
            return false;
        }
        CompletableFuture.runAsync(() -> {
            try {
                doFullSync();
            } catch (Exception e) {
                log.error("Manual NVD full sync failed", e);
            } finally {
                syncRunning.set(false);
            }
        });
        return true;
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
            doFullSync();
        } finally {
            syncRunning.set(false);
        }
    }

    private void doFullSync() {
        log.info("Starting full NVD sync");
        syncCves(null); // null means fetch all
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
            syncCves(lastModified.plusSeconds(3));

            log.info("Completed incremental NVD sync");
        } finally {
            syncRunning.set(false);
        }
    }

    private void syncCves(LocalDateTime lastModStartDate) {
        int startIndex = 0;
        int totalResults = 0;
        int rateLimitRetries = 0;
        long pageDelayMs = hasApiKey() ? PAGE_DELAY_WITH_KEY_MS : PAGE_DELAY_NO_KEY_MS;

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
                sleepQuietly(pageDelayMs);

            } catch (HttpClientErrorException.TooManyRequests e) {
                // NVD rate limit with an API key.
                if (++rateLimitRetries > MAX_RATE_LIMIT_RETRIES) {
                    log.error("NVD rate limit hit repeatedly; aborting sync");
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
                    log.error("NVD quota exceeded repeatedly; aborting sync");
                    break;
                }
                log.warn(
                    "NVD quota exceeded (403); backing off {} ms before retrying startIndex {}",
                    RATE_LIMIT_BACKOFF_MS, startIndex
                );
                sleepQuietly(RATE_LIMIT_BACKOFF_MS);
            } catch (Exception e) {
                log.error("Error syncing CVEs at startIndex {}: {}", startIndex, e.getMessage());
                break;
            }
        } while (startIndex < totalResults);
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
                            String vendor = cpeParts[3];
                            String product = cpeParts[4];

                            LocalCve localCve = LocalCve.builder()
                                .cveId(cveId)
                                .cpeProduct(product.toLowerCase())
                                .cpeVendor(vendor.toLowerCase())
                                .description(extractDescription(cveNode))
                                .cvssScore(extractCvssScore(cveNode))
                                .severity(extractSeverity(cveNode))
                                .vectorString(extractVectorString(cveNode))
                                .versionStartIncluding(
                                    match.has("versionStartIncluding")
                                        ? match.get("versionStartIncluding").asText()
                                        : null
                                )
                                .versionStartExcluding(
                                    match.has("versionStartExcluding")
                                        ? match.get("versionStartExcluding").asText()
                                        : null
                                )
                                .versionEndIncluding(
                                    match.has("versionEndIncluding")
                                        ? match.get("versionEndIncluding").asText()
                                        : null
                                )
                                .versionEndExcluding(
                                    match.has("versionEndExcluding")
                                        ? match.get("versionEndExcluding").asText()
                                        : null
                                )
                                .publishedDate(parseDate(cveNode, "published"))
                                .lastModified(parseDate(cveNode, "lastModified"))
                                .rawJson(cveNode.toString())
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

        if (hasApiKey()) {
            builder.queryParam("apiKey", nvdApiKey);
        }

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