package com.vulntrack.service;

import com.vulntrack.dto.CveResult;
import com.vulntrack.dto.NvdCveResponse;
import com.vulntrack.dto.NvdCveResponse.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class NvdCveByCpeClientService {

    private static final int PAGE_SIZE = 100;
    private static final String NVD_BASE_URL = "https://services.nvd.nist.gov/rest/json/cves/2.0";

    private final RestClient restClient = RestClient.builder()
        .baseUrl(NVD_BASE_URL)
        .build();

    public List<CveResult> fetchCvesByCpe(String cpeName) {
        List<CveResult> allResults = new ArrayList<>();
        int startIndex = 0;
        int totalResults = 0;
        boolean firstPage = true;

        log.info("Starting CVE fetch for CPE: {}", cpeName);

        do {
            final int currentPageIndex = startIndex;

            try {
                NvdCveResponse page = restClient.get()
                    .uri(
                        u -> u
                            .queryParam("cpeName", cpeName)
                            .queryParam("resultsPerPage", PAGE_SIZE)
                            .queryParam("startIndex", currentPageIndex)
                            .build()
                    )
                    .retrieve()
                    .body(NvdCveResponse.class);

                if (page == null || page.getVulnerabilities() == null) {
                    log.warn("Empty response from NVD for CPE: {}", cpeName);
                    break;
                }

                // Capture total results from the FIRST page only
                if (firstPage) {
                    totalResults = page.getTotalResults();
                    firstPage = false;
                    log.info("Total CVEs to fetch: {}", totalResults);
                }

                // Map results using stream (no startIndex reference inside lambda)
                List<CveResult> mappedPage = page.getVulnerabilities().stream()
                    .map(v -> toResult(v.getCve()))
                    .toList();

                allResults.addAll(mappedPage);

                // Increment startIndex AFTER processing
                startIndex += PAGE_SIZE;

                log.debug("Fetched {}/{} CVEs", allResults.size(), totalResults);

            } catch (Exception e) {
                log.error(
                    "Error fetching page starting at index {}: {}", currentPageIndex, e.getMessage()
                );
                break; // Stop on error to avoid infinite loop
            }

        } while (startIndex < totalResults);

        log.info("Completed fetch for {}. Total found: {}", cpeName, allResults.size());
        return allResults;
    }

    // ── Mapping Logic
    // ───────────────────────────────────────────────────────────────

    private static CveResult toResult(Cve cve) {
        String description = extractEnglishDescription(cve);
        String cwe = extractFirstCwe(cve);
        ScoreInfo score = extractBestScore(cve);

        return new CveResult(
            cve.getId(),
            description,
            score.baseScore(),
            score.version(),
            score.severity(),
            score.vectorString(),
            cve.getPublished(),
            cwe
        );
    }

    private static String extractEnglishDescription(Cve cve) {
        if (cve.getDescriptions() == null || cve.getDescriptions().isEmpty())
            return "No description available.";

        return cve.getDescriptions().stream()
            .filter(d -> "en".equalsIgnoreCase(d.getLang()))
            .map(NvdCveResponse.Description::getValue)
            .findFirst()
            .orElse(cve.getDescriptions().get(0).getValue());
    }

    private static String extractFirstCwe(Cve cve) {
        if (cve.getWeaknesses() == null)
            return null;

        return cve.getWeaknesses().stream()
            .flatMap(
                w -> w.getDescription() != null ? w.getDescription().stream()
                    : java.util.stream.Stream.empty()
            )
            .map(NvdCveResponse.Description::getValue)
            .filter(val -> val != null && val.startsWith("CWE-"))
            .findFirst()
            .orElse(null);
    }

    private static ScoreInfo extractBestScore(Cve cve) {
        if (cve.getMetrics() == null)
            return ScoreInfo.empty();

        // Priority 1: CVSS v3.1
        if (cve.getMetrics().getCvssMetricV31() != null
            && !cve.getMetrics().getCvssMetricV31().isEmpty()) {
            CvssMetricV31 metric = cve.getMetrics().getCvssMetricV31().get(0);
            if (metric.getCvssData() != null) {
                return new ScoreInfo(
                    metric.getCvssData().getBaseScore(),
                    "3.1",
                    metric.getCvssData().getBaseSeverity(),
                    metric.getCvssData().getVectorString()
                );
            }
        }

        // Priority 2: CVSS v2
        if (cve.getMetrics().getCvssMetricV2() != null
            && !cve.getMetrics().getCvssMetricV2().isEmpty()) {
            CvssMetricV2 metric = cve.getMetrics().getCvssMetricV2().get(0);
            if (metric.getCvssData() != null) {
                return new ScoreInfo(
                    metric.getCvssData().getBaseScore(),
                    "2.0",
                    metric.getBaseSeverity(), // V2 severity is on the metric object
                    metric.getCvssData().getVectorString()
                );
            }
        }

        return ScoreInfo.empty();
    }

    private record ScoreInfo(
        Double baseScore,
        String version,
        String severity,
        String vectorString
    ) {
        static ScoreInfo empty() { return new ScoreInfo(null, null, null, null); }
    }
}