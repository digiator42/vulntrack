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
        int startIndex = 92000;
        int totalResults = 0;

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

                startIndex += 1000; // Results per page

            } catch (Exception e) {
                log.error("Error syncing CVEs at startIndex {}: {}", startIndex, e.getMessage());
                break;
            }
        } while (startIndex < totalResults);
    }

    private void processCveNode(JsonNode cveNode, List<LocalCve> cves, int startIndex) {
        try {
            String cveId = cveNode.get("id").asText();

            // Check if CVE already exists in the current batch or database
            boolean alreadyExists = cves.stream().anyMatch(c -> c.getCveId().equals(cveId)) ||
                localCveRepository.findByCveId(cveId).isPresent();

            System.out.println("====> CVE ID: " + cveId);
            if (alreadyExists) {
                System.out.println("==============>>> Skipping duplicate CVE: " + cveId);
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
                                .rawJson(String.valueOf(startIndex))
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
            .queryParam("resultsPerPage", 1000)
            .queryParam("startIndex", startIndex);

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