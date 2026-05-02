package com.vulntrack.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vulntrack.dto.CveVulnerability;
import com.vulntrack.dto.NvdCpeResponse;
import com.vulntrack.dto.ParsedDependency;
import com.vulntrack.model.CpeCache;
import com.vulntrack.model.DependencyVulnerabilityCache;
import com.vulntrack.model.LocalCve;
import com.vulntrack.repository.CpeCacheRepository;
import com.vulntrack.repository.DependencyVulnerabilityCacheRepository;
import com.vulntrack.repository.LocalCveRepository;

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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NvdClientService {

    private final LocalCveRepository localCveRepository;

    @Value("${nvd.api.url:https://services.nvd.nist.gov/rest/json/cves/2.0}")
    private String nvdApiUrl;

    private static final int CACHE_TTL_HOURS = 24;
    private static final int CPE_CACHE_DAYS = 30; // CPE mappings don't change often

    /**
     * Check a dependency for vulnerabilities using local database
     */
    public List<CveVulnerability> checkDependency(ParsedDependency dep, String manifestFile) {
        String cleanVersion = cleanVersion(dep.version());
        String productName = extractPackageName(dep);

        // Query local database for vulnerabilities affecting this product/version
        List<LocalCve> localCves = localCveRepository.findVulnerabilitiesForVersion(
            productName.toLowerCase(),
            cleanVersion
        );

        return localCves.stream()
            .map(
                cve -> new CveVulnerability(
                    cve.getCveId(),
                    cve.getDescription(),
                    cve.getCvssScore(),
                    cve.getSeverity(),
                    cve.getPublishedDate() != null ? cve.getPublishedDate().toString() : null,
                    cve.getVectorString()
                )
            )
            .collect(Collectors.toList());
    }

    private String cleanVersion(String version) {
        if (version == null)
            return null;
        String cleaned = version.replaceFirst("^[\\^~><=]", "");
        if (cleaned.contains(" ")) {
            String[] parts = cleaned.split("\\s+");
            for (String part : parts) {
                if (part.matches("\\d+(\\.\\d+)*.*")) {
                    cleaned = part;
                    break;
                }
            }
        }
        return cleaned;
    }

    private String extractPackageName(ParsedDependency dep) {
        String name = dep.name();
        if (name.contains(":")) {
            String[] parts = name.split(":");
            return parts[parts.length - 1];
        }
        return name;
    }

    private boolean isVersionInRange(
        ComparableVersion current,
        String startIncluding,
        String startExcluding,
        String endIncluding,
        String endExcluding
    ) {
        if (startIncluding != null
            && current.compareTo(new ComparableVersion(startIncluding)) < 0) {
            return false;
        }
        if (startExcluding != null
            && current.compareTo(new ComparableVersion(startExcluding)) <= 0) {
            return false;
        }
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
        }

        return results;
    }
}