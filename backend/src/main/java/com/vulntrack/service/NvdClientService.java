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

    public List<LocalCve> smartLookUp(
        List<LocalCve> allCves,
        String productName,
        String ecosystem
    ) {
        // Strategy 2: try partial match (contains)
        allCves
            .addAll(localCveRepository.findByCpeProductContaining(productName.toLowerCase()));

        // Strategy 3: Try the first part of the name (for multi-part names like
        // "log4j-core" -> "log4j")
        if (allCves.isEmpty() && productName.contains("-")) {
            String simplifiedName = productName.split("-")[0];
            System.out.println("Trying simplified name: '" + simplifiedName + "'");
            allCves.addAll(localCveRepository.findByCpeProduct(simplifiedName.toLowerCase()));
        }

        allCves.addAll(
            localCveRepository.findByProductOrVendorContaining(productName.toLowerCase())
        );

        // Strategy 4: checking if the product name appears in description
        if (allCves.isEmpty()) {
            allCves
                .addAll(localCveRepository.findByDescriptionContaining(productName.toLowerCase()));
        }

        if (allCves.isEmpty()) {
            System.out.println("No CVEs found for product: " + productName);
            return Collections.emptyList();
        }
        return allCves;
    }

    /**
     * Check a dependency for vulnerabilities using local database
     */
    public List<CveVulnerability> checkDependency(ParsedDependency dep, String manifestFile) {
        String cleanVersion = cleanVersion(dep.version());
        String productName = extractPackageName(dep);
        String ecosystem = dep.ecosystem();

        System.out.println(
            "Checking vulnerabilities for " + dep.name() + ": " + cleanVersion + " product: " +
                productName + ", ecosystem: " + ecosystem
        );

        // Get ALL CVEs for this product
        List<LocalCve> allCves = localCveRepository.findByCpeProduct(productName.toLowerCase());

        smartLookUp(allCves, productName, ecosystem);

        System.out.println(
            "FOUND ========> " + allCves.size() + " " + productName
        );

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
                    System.out.println(
                        "CVE {} affects version {} of {} (range: {}-{})" + " " +
                            cveId + " " + cleanVersion + " " + productName + " " +
                            cve.getVersionStartIncluding() != null ? cve.getVersionStartIncluding()
                                : "any" + " " +
                                    cve.getVersionEndExcluding() != null
                                        ? cve.getVersionEndExcluding()
                                        : "any"
                    );
                } else {
                    System.out.println(
                        "CVE {} does NOT affect version {} of {} (version out of range)" + " " +
                            cveId + " " + cleanVersion + " " + productName
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
        String cveProduct = cve.getCpeProduct().toLowerCase();
        String targetProduct = productName.toLowerCase();

        // 1. Exact vs. Fuzzy Product Matching
        if (cveProduct.equals(targetProduct)) {
            score += 100;
        } else if (targetProduct.replace("-", "_").equals(cveProduct)) {
            score += 90; // Handles Python/NPM naming variations
        } else if (cveProduct.contains(targetProduct) || targetProduct.contains(cveProduct)) {
            score += 40; // Partial match
        }

        // 2. Ecosystem-Specific Vendor Confidence
        switch (ecosystem.toUpperCase()) {
        case "MAVEN":
            String groupId = extractGroupId(productName); // e.g., org.apache.logging.log4j
            if (groupId != null && cve.getCpeVendor().equalsIgnoreCase(groupId)) {
                score += 50;
            }
            break;
        case "NPM":
            if (productName.startsWith("@")) {
                String scope = productName.split("/")[0].substring(1);
                if (cve.getCpeVendor().equalsIgnoreCase(scope)) {
                    score += 50; // High confidence if scope matches NVD vendor
                }
            }
            break;
        case "PYPI":
            // PyPI vendors are often just the product name or the author
            if (cve.getCpeVendor().equalsIgnoreCase(targetProduct)) {
                score += 30;
            }
            break;
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

            System.out.println("========== Checking CVE: " + cve.getCveId());
            System.out.println("  Current version: " + currentVersion);
            System.out.println("  versionStartIncluding: " + cve.getVersionStartIncluding());
            System.out.println("  versionStartExcluding: " + cve.getVersionStartExcluding());
            System.out.println("  versionEndIncluding: " + cve.getVersionEndIncluding());
            System.out.println("  versionEndExcluding: " + cve.getVersionEndExcluding());

            if (cve.getVersionStartIncluding() == null &&
                cve.getVersionStartExcluding() == null &&
                cve.getVersionEndIncluding() == null &&
                cve.getVersionEndExcluding() == null) {

                // LOGIC: If it's an exact match on product name and no ranges exist,
                // we flag it as vulnerable to avoid a false negative.
                System.out.println(
                    "CVE {" + cve.getCveId()
                        + "} has no version ranges. Flagging as potentially vulnerable."
                );
                return true;
            }

            // Check lower bound
            if (cve.getVersionStartIncluding() != null) {
                ComparableVersion startInc = new ComparableVersion(cve.getVersionStartIncluding());
                int cmp = current.compareTo(startInc);
                System.out.println(
                    "  startInc check: " + current + " >= " + startInc + "? " + (cmp >= 0)
                );
                if (cmp < 0) {
                    System.out.println("  -> NOT affected (below start including)");
                    return false;
                }
            }

            if (cve.getVersionStartExcluding() != null) {
                ComparableVersion startExc = new ComparableVersion(cve.getVersionStartExcluding());
                int cmp = current.compareTo(startExc);
                System.out
                    .println("  startExc check: " + current + " > " + startExc + "? " + (cmp > 0));
                if (cmp <= 0) {
                    System.out.println("  -> NOT affected (below or equal start excluding)");
                    return false;
                }
            }

            // Check upper bound
            if (cve.getVersionEndIncluding() != null) {
                ComparableVersion endInc = new ComparableVersion(cve.getVersionEndIncluding());
                int cmp = current.compareTo(endInc);
                System.out
                    .println("  endInc check: " + current + " <= " + endInc + "? " + (cmp <= 0));
                if (cmp > 0) {
                    System.out.println("  -> NOT affected (above end including)");
                    return false;
                }
            }

            if (cve.getVersionEndExcluding() != null) {
                ComparableVersion endExc = new ComparableVersion(cve.getVersionEndExcluding());
                int cmp = current.compareTo(endExc);
                System.out
                    .println("  endExc check: " + current + " < " + endExc + "? " + (cmp < 0));
                if (cmp >= 0) {
                    System.out.println("  -> NOT affected (above or equal end excluding)");
                    return false;
                }
            }

            System.out.println("  -> AFFECTED!");
            return true;

        } catch (Exception e) {
            System.out.println("  Error: " + e.getMessage());
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