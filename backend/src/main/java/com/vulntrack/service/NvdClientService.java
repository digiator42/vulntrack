package com.vulntrack.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.vulntrack.dto.CveVulnerability;
import com.vulntrack.dto.ParsedDependency;
import com.vulntrack.model.DependencyVulnerabilityCache;
import com.vulntrack.repository.DependencyVulnerabilityCacheRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class NvdClientService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private final DependencyVulnerabilityCacheRepository cacheRepository;

    @Value("${nvd.api.url:https://services.nvd.nist.gov/rest/json/cves/2.0}")
    private String nvdApiUrl;

    private static final int CACHE_TTL_HOURS = 24;

    public List<CveVulnerability> checkDependency(ParsedDependency dep, String manifestFile) {

        Optional<DependencyVulnerabilityCache> cached = cacheRepository
            .findByDependencyNameAndDependencyVersion(dep.name(), dep.version());

        if (cached.isPresent()) {
            DependencyVulnerabilityCache cache = cached.get();
            if (cache.getLastChecked().plusHours(CACHE_TTL_HOURS).isAfter(LocalDateTime.now())) {
                log.info("Using cached result for {}:{}", dep.name(), dep.version());
                if (!cache.isHasVulnerabilities()) {
                    return Collections.emptyList();
                }
                try {
                    return objectMapper.readValue(
                        cache.getVulnerabilityData(),
                        objectMapper.getTypeFactory()
                            .constructCollectionType(List.class, CveVulnerability.class)
                    );
                } catch (Exception e) {
                    log.error("Error parsing cached data", e);
                }
            }
        }

        List<CveVulnerability> vulnerabilities = fetchFromNvd(dep);

        try {
            String jsonData = objectMapper.writeValueAsString(vulnerabilities);
            DependencyVulnerabilityCache cache = DependencyVulnerabilityCache.builder()
                .dependencyName(dep.name())
                .dependencyVersion(dep.version())
                .vulnerabilityData(jsonData)
                .lastChecked(LocalDateTime.now())
                .hasVulnerabilities(!vulnerabilities.isEmpty())
                .build();
            cacheRepository.save(cache);
        } catch (Exception e) {
            log.error("Error saving to cache", e);
        }

        return vulnerabilities;
    }

    private List<CveVulnerability> fetchFromNvd(ParsedDependency dep) {
        try {
            String keyword = extractKeywordForSearch(dep);
            String encodedKeyword = URLEncoder.encode(keyword, StandardCharsets.UTF_8);

            String url = UriComponentsBuilder.fromHttpUrl(nvdApiUrl)
                .queryParam("keywordSearch", encodedKeyword)
                .queryParam("resultsPerPage", 50)
                .build()
                .toUriString();

            log.info("Querying NVD API: {}", url);

            String response = restTemplate.getForObject(url, String.class);
            log.info("Done Querying NVD API: {}", url);
            JsonNode root = objectMapper.readTree(response);

            List<CveVulnerability> matchingVulnerabilities = new ArrayList<>();
            JsonNode vulnerabilities = root.get("vulnerabilities");

            if (vulnerabilities != null && vulnerabilities.isArray()) {
                for (JsonNode vulnNode : vulnerabilities) {
                    JsonNode cve = vulnNode.get("cve");
                    String cveId = cve.get("id").asText();

                    if (isAffectedByCve(cve, dep)) {
                        CveVulnerability vuln = new CveVulnerability(
                            cveId,
                            extractDescription(cve),
                            extractCvssScore(cve),
                            extractSeverity(cve),
                            extractDate(cve, "published"),
                            extractVectorString(cve)
                        );

                        matchingVulnerabilities.add(vuln);
                        log.info(
                            "Found vulnerability {} for {}:{}", cveId, dep.name(),
                            dep.version()
                        );
                    }
                }
            }

            return matchingVulnerabilities;

        } catch (Exception e) {
            log.error("Error fetching from NVD for {}:{}", dep.name(), dep.version(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Check if current version is affected by this CVE based on version ranges
     */
    private boolean isAffectedByCve(JsonNode cveNode, ParsedDependency dep) {
        try {
            JsonNode configurations = cveNode.get("configurations");
            if (configurations == null)
                return false;

            ComparableVersion current = new ComparableVersion(dep.version());
            String packageName = extractPackageName(dep); // e.g., "snakeyaml"

            for (JsonNode config : configurations) {
                JsonNode nodes = config.get("nodes");
                if (nodes != null) {
                    for (JsonNode node : nodes) {
                        JsonNode cpeMatches = node.get("cpeMatch");
                        if (cpeMatches != null) {
                            for (JsonNode match : cpeMatches) {
                                boolean vulnerable = match.has("vulnerable")
                                    && match.get("vulnerable").asBoolean();
                                if (!vulnerable)
                                    continue;

                                // CRITICAL: Check if CPE matches our package
                                String criteria = match.get("criteria").asText().toLowerCase();
                                if (!criteria.contains(packageName.toLowerCase())) {
                                    continue; // This CVE is for a different package!
                                }

                                // Extract version ranges
                                String versionStartIncluding = match.has("versionStartIncluding")
                                    ? match.get("versionStartIncluding").asText()
                                    : null;
                                String versionStartExcluding = match.has("versionStartExcluding")
                                    ? match.get("versionStartExcluding").asText()
                                    : null;
                                String versionEndIncluding = match.has("versionEndIncluding")
                                    ? match.get("versionEndIncluding").asText()
                                    : null;
                                String versionEndExcluding = match.has("versionEndExcluding")
                                    ? match.get("versionEndExcluding").asText()
                                    : null;

                                // Log for debugging
                                System.out.println(
                                    "========> " + current + " " +
                                        versionStartIncluding + " " + versionStartExcluding + " " +
                                        versionEndIncluding + " " + versionEndExcluding
                                );

                                // If all bounds are null, assume ALL versions are vulnerable
                                if (versionStartIncluding == null && versionStartExcluding == null
                                    &&
                                    versionEndIncluding == null && versionEndExcluding == null) {
                                    log.info("CVE with no version bounds - ALL versions affected");
                                    return true;
                                }

                                // Check version range
                                if (isVersionInRange(
                                    current, versionStartIncluding, versionStartExcluding,
                                    versionEndIncluding, versionEndExcluding
                                )) {
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error checking version for CVE", e);
        }
        return false;
    }

    private String extractPackageName(ParsedDependency dep) {
        String name = dep.name();
        // For Maven: "org.yaml:snakeyaml" -> "snakeyaml"
        if (name.contains(":")) {
            String[] parts = name.split(":");
            return parts[parts.length - 1];
        }
        // For npm, pypi, etc.
        return name;
    }

    private boolean isVersionInRange(
        ComparableVersion current,
        String startIncluding,
        String startExcluding,
        String endIncluding,
        String endExcluding
    ) {
        // Check lower bound
        if (startIncluding != null
            && current.compareTo(new ComparableVersion(startIncluding)) < 0) {
            return false;
        }
        if (startExcluding != null
            && current.compareTo(new ComparableVersion(startExcluding)) <= 0) {
            return false;
        }

        // Check upper bound
        if (endIncluding != null && current.compareTo(new ComparableVersion(endIncluding)) > 0) {
            return false;
        }
        if (endExcluding != null && current.compareTo(new ComparableVersion(endExcluding)) >= 0) {
            return false;
        }

        return true;
    }

    private String extractKeywordForSearch(ParsedDependency dep) {
        String name = dep.name();
        if (name.contains(":")) {
            String[] parts = name.split(":");
            return parts[parts.length - 1]; // Return artifactId only
        }
        return name;
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
                JsonNode cvssV3 = metrics.get("cvssMetricV30");
                if (cvssV3 != null && cvssV3.isArray() && cvssV3.size() > 0) {
                    return cvssV3.get(0).get("cvssData").get("baseScore").asDouble();
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

    private String extractDate(JsonNode cveNode, String field) {
        try {
            if (cveNode.has(field)) {
                return cveNode.get(field).asText();
            }
        } catch (Exception e) {
            log.debug("Could not extract date for field: {}", field);
        }
        return null;
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

    public Map<ParsedDependency, List<CveVulnerability>> checkDependencies(
        List<ParsedDependency> deps,
        String manifestFile
    ) {
        Map<ParsedDependency, List<CveVulnerability>> results = new HashMap<>();

        for (ParsedDependency dep : deps) {
            List<CveVulnerability> vulns = checkDependency(dep, manifestFile);
            if (!vulns.isEmpty()) {
                results.put(dep, vulns);
            }
            try {
                TimeUnit.MILLISECONDS.sleep(500); // Rate limiting
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        return results;
    }
}