package com.vulntrack.service;

import com.vulntrack.dto.CveVulnerability;
import com.vulntrack.dto.ParsedDependency;
import com.vulntrack.model.LocalCve;
import com.vulntrack.repository.LocalCveRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NvdClientService {

    private final LocalCveRepository localCveRepository;

    /**
     * Check a dependency for vulnerabilities using local database
     */
    public List<CveVulnerability> checkDependency(ParsedDependency dep, String manifestFile) {
        String cleanVersion = cleanVersion(dep.version());
        String productName = extractPackageName(dep);
        String ecosystem = dep.ecosystem();

        log.debug(
            "Checking vulnerabilities for {}:{} (product: {}, ecosystem: {})",
            dep.name(), cleanVersion, productName, ecosystem
        );

        // Get ALL CVEs for this product
        List<LocalCve> allCves = localCveRepository.findByCpeProduct(productName.toLowerCase());

        if (allCves.isEmpty()) {
            log.debug("No CVEs found for product: {}", productName);
            return Collections.emptyList();
        }

        log.debug("Found {} total CVE records for product {}", allCves.size(), productName);

        // Group by CVE ID and find the best matching CPE for each CVE
        Map<String, Optional<LocalCve>> bestMatchPerCve = groupAndFindBestMatch(
            allCves, productName, ecosystem, cleanVersion
        );

        // Convert to vulnerabilities, filtering out those not affected by version
        List<CveVulnerability> vulnerabilities = new ArrayList<>();

        for (Map.Entry<String, Optional<LocalCve>> entry : bestMatchPerCve.entrySet()) {
            String cveId = entry.getKey();
            Optional<LocalCve> bestMatch = entry.getValue();

            if (bestMatch.isPresent()) {
                LocalCve cve = bestMatch.get();

                // Check if the version is affected by this CVE
                if (isVersionAffected(cve, cleanVersion)) {
                    vulnerabilities.add(
                        new CveVulnerability(
                            cve.getCveId(),
                            cve.getDescription(),
                            cve.getCvssScore(),
                            cve.getSeverity(),
                            cve.getPublishedDate() != null ? cve.getPublishedDate().toString()
                                : null,
                            cve.getVectorString(),
                            cve.getVersionStartIncluding(),
                            cve.getVersionStartExcluding(),
                            cve.getVersionEndIncluding(),
                            cve.getVersionEndExcluding()
                        )
                    );
                    log.debug(
                        "CVE {} affects version {} of {} (range: {}-{})",
                        cveId, cleanVersion, productName,
                        cve.getVersionStartIncluding() != null ? cve.getVersionStartIncluding()
                            : "any",
                        cve.getVersionEndExcluding() != null ? cve.getVersionEndExcluding() : "any"
                    );
                } else {
                    log.debug(
                        "CVE {} does NOT affect version {} of {} (version out of range)",
                        cveId, cleanVersion, productName
                    );
                }
            }
        }

        log.info(
            "Found {} vulnerabilities for {}:{}", vulnerabilities.size(), dep.name(), cleanVersion
        );
        return vulnerabilities;
    }

    /**
     * Group CVEs by ID and find the best matching CPE for each
     */
    private Map<String, Optional<LocalCve>> groupAndFindBestMatch(
        List<LocalCve> allCves,
        String productName,
        String ecosystem,
        String version
    ) {

        Map<String, List<LocalCve>> groupedByCveId = allCves.stream()
            .collect(Collectors.groupingBy(LocalCve::getCveId));

        Map<String, Optional<LocalCve>> bestMatches = new HashMap<>();

        for (Map.Entry<String, List<LocalCve>> entry : groupedByCveId.entrySet()) {
            String cveId = entry.getKey();
            List<LocalCve> cveEntries = entry.getValue();

            // Score and pick the best matching CPE for this CVE
            Optional<LocalCve> bestMatch = cveEntries.stream()
                .max((c1, c2) -> {
                    int score1 = calculateMatchScore(c1, productName, ecosystem, version);
                    int score2 = calculateMatchScore(c2, productName, ecosystem, version);
                    return Integer.compare(score1, score2);
                });

            bestMatches.put(cveId, bestMatch);
        }

        return bestMatches;
    }

    /**
     * Calculate how well a CVE entry matches our dependency Higher score = better
     * match
     */
    private int calculateMatchScore(
        LocalCve cve,
        String productName,
        String ecosystem,
        String version
    ) {
        int score = 0;

        // Exact product match (highest priority)
        if (cve.getCpeProduct().equals(productName.toLowerCase())) {
            score += 100;
        } else if (cve.getCpeProduct().contains(productName.toLowerCase())) {
            score += 50;
        } else if (productName.toLowerCase().contains(cve.getCpeProduct())) {
            score += 30;
        }

        // Vendor matching for Maven (groupId)
        if ("MAVEN".equals(ecosystem)) {
            String expectedVendor = extractGroupId(productName);
            if (expectedVendor != null && cve.getCpeVendor().equals(expectedVendor.toLowerCase())) {
                score += 40;
            }
        }

        // Version range specificity - prefer entries with version bounds that match
        boolean hasStartBound = cve.getVersionStartIncluding() != null
            || cve.getVersionStartExcluding() != null;
        boolean hasEndBound = cve.getVersionEndIncluding() != null
            || cve.getVersionEndExcluding() != null;

        if (hasStartBound && hasEndBound) {
            score += 30; // Both bounds - very specific
        } else if (hasStartBound || hasEndBound) {
            score += 15; // One bound - moderately specific
        }

        // Bonus if the version actually falls within the range
        if (isVersionAffected(cve, version)) {
            score += 20;
        }

        return score;
    }

    /**
     * Extract groupId from Maven dependency name e.g., "org.yaml:snakeyaml" ->
     * "org.yaml"
     */
    private String extractGroupId(String productName) {
        if (productName.contains(":")) {
            return productName.split(":")[0];
        }
        return null;
    }

    /**
     * Check if a specific version is affected by this CVE
     */
    private boolean isVersionAffected(LocalCve cve, String currentVersion) {
        try {
            ComparableVersion current = new ComparableVersion(currentVersion);

            // Check lower bound
            if (cve.getVersionStartIncluding() != null) {
                ComparableVersion startInc = new ComparableVersion(cve.getVersionStartIncluding());
                if (current.compareTo(startInc) < 0)
                    return false;
            }

            if (cve.getVersionStartExcluding() != null) {
                ComparableVersion startExc = new ComparableVersion(cve.getVersionStartExcluding());
                if (current.compareTo(startExc) <= 0)
                    return false;
            }

            // Check upper bound
            if (cve.getVersionEndIncluding() != null) {
                ComparableVersion endInc = new ComparableVersion(cve.getVersionEndIncluding());
                // For exact version: current == 1.7.2 AND endInc == 1.7.2 → true only if
                // current == 1.7.2
                if (current.compareTo(endInc) > 0)
                    return false;
            }

            if (cve.getVersionEndExcluding() != null) {
                ComparableVersion endExc = new ComparableVersion(cve.getVersionEndExcluding());
                if (current.compareTo(endExc) >= 0)
                    return false;
            }

            // If no version bounds, check if this CVE is version-specific via description
            if (cve.getVersionStartIncluding() == null && cve.getVersionStartExcluding() == null &&
                cve.getVersionEndIncluding() == null && cve.getVersionEndExcluding() == null) {
                // Check if description mentions a specific version
                String description = cve.getDescription();
                if (description != null
                    && description.matches(".*\\b" + currentVersion + "\\b.*")) {
                    return true;
                }
                log.warn(
                    "CVE {} has no version bounds - manual review recommended for version {}",
                    cve.getCveId(), currentVersion
                );
                return false; // Conservative: don't mark vulnerable without bounds
            }

            return true;

        } catch (Exception e) {
            log.error("Error checking version for CVE {}: {}", cve.getCveId(), e.getMessage());
            return false;
        }
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

    public Map<ParsedDependency, List<CveVulnerability>> checkDependencies(
        List<ParsedDependency> deps,
        String manifestFile
    ) {

        Map<ParsedDependency, List<CveVulnerability>> results = new HashMap<>();

        for (ParsedDependency dep : deps) {
            List<CveVulnerability> vulns = checkDependency(dep, manifestFile);
            if (!vulns.isEmpty()) {
                results.put(dep, vulns);
                log.info(
                    "Found {} vulnerabilities for dependency {}:{}",
                    vulns.size(), dep.name(), dep.version()
                );
            }
        }

        return results;
    }
}