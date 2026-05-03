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
public class NvdClientService2 {

    private final LocalCveRepository localCveRepository;
    private final DependencyNormalizer dependencyNormalizer;

    /**
     * THREE-LEVEL HYBRID ARCHITECTURE:
     * 
     * Level 1: Authoritative CPE Database (DependencyNormalizer with
     * package_cpe_mapping) Level 2: Heuristic Generation (ecosystem-specific rules)
     * Level 3: Fuzzy Fallback (scoring system + partial matching)
     */
    public List<CveVulnerability> checkDependency(ParsedDependency dep, String manifestFile) {
        String cleanVersion = cleanVersion(dep.version());
        String ecosystem = dep.ecosystem();
        String fullName = dep.name();

        System.out.println("========================================");
        System.out.println("Checking: " + fullName + " version: " + cleanVersion);
        System.out.println("Ecosystem: " + ecosystem);

        // == LEVEL 1: AUTHORITATIVE CPE DATABASE (via DependencyNormalizer) ==
        var normalized = dependencyNormalizer.normalize(dep);

        if (!normalized.cpeProduct().equals(normalized.originalName())) {
            log.info(
                "✓ Normalized: {} -> {}:{} via {}",
                normalized.originalName(),
                normalized.cpeVendor(),
                normalized.cpeProduct(),
                normalized.matchReason()
            );
        }

        // Now lookup CVEs using the normalized vendor+product
        List<LocalCve> matchedCves = localCveRepository.findByCpeVendorAndCpeProduct(
            normalized.cpeVendor().toLowerCase(),
            normalized.cpeProduct().toLowerCase()
        );

        // Fallback: if no exact vendor+product match, try product-only
        // if (matchedCves.isEmpty()) {
        // matchedCves = localCveRepository
        // .findByCpeProduct(normalized.cpeProduct().toLowerCase());
        // }

        // Deduplicate by CVE ID
        matchedCves = matchedCves.stream()
            .collect(Collectors.toMap(LocalCve::getCveId, c -> c, (c1, c2) -> c1))
            .values()
            .stream()
            .collect(Collectors.toList());

        // Group by CVE ID and find the best matching CPE using scoring system
        Map<String, Optional<LocalCve>> bestMatchPerCve = groupAndFindBestMatch(
            matchedCves, fullName, ecosystem, cleanVersion
        );

        // Convert to vulnerabilities
        List<CveVulnerability> vulnerabilities = new ArrayList<>();

        for (Map.Entry<String, Optional<LocalCve>> entry : bestMatchPerCve.entrySet()) {
            Optional<LocalCve> bestMatch = entry.getValue();
            if (bestMatch.isPresent() && isVersionAffected(bestMatch.get(), cleanVersion)) {
                LocalCve cve = bestMatch.get();
                vulnerabilities.add(
                    new CveVulnerability(
                        cve.getCveId(),
                        cve.getDescription(),
                        cve.getCvssScore(),
                        cve.getSeverity(),
                        cve.getPublishedDate() != null ? cve.getPublishedDate().toString() : null,
                        cve.getVectorString(),
                        cve.getVersionStartIncluding(),
                        cve.getVersionStartExcluding(),
                        cve.getVersionEndIncluding(),
                        cve.getVersionEndExcluding()
                    )
                );
            }
        }

        System.out.println("RESULT: " + vulnerabilities.size() + " vulnerabilities found");
        System.out.println("========================================");

        return vulnerabilities;
    }

    /**
     * LEVEL 2: Heuristic Generation Ecosystem-specific rules without hardcoded
     * product names
     */
    private List<LocalCve> lookupByHeuristics(String fullName, String ecosystem) {
        List<LocalCve> results = new ArrayList<>();

        switch (ecosystem.toUpperCase()) {
        case "MAVEN":
            if (fullName.contains(":")) {
                String[] parts = fullName.split(":");
                String groupId = parts[0];
                String artifactId = parts[1];

                // Heuristic 1: Try artifactId directly
                List<LocalCve> artifactMatches = localCveRepository
                    .findByCpeProduct(artifactId.toLowerCase());
                if (!artifactMatches.isEmpty()) {
                    results.addAll(artifactMatches);
                    System.out.println("  Heuristic: artifactId '" + artifactId + "' matched");
                }

                // Heuristic 2: Try last part of groupId as vendor
                String vendorHint = extractVendorFromGroupId(groupId);
                if (!vendorHint.isEmpty() && results.isEmpty()) {
                    List<LocalCve> vendorMatches = localCveRepository
                        .findByCpeVendorContaining(vendorHint);
                    if (!vendorMatches.isEmpty()) {
                        results.addAll(vendorMatches);
                        System.out.println("  Heuristic: vendor '" + vendorHint + "' matched");
                    }
                }
            }
            break;

        case "NPM":
            String npmName = fullName;
            if (npmName.startsWith("@")) {
                npmName = npmName.substring(1);
                if (npmName.contains("/")) {
                    npmName = npmName.split("/")[1];
                }
            }
            List<LocalCve> npmMatches = localCveRepository.findByCpeProduct(npmName.toLowerCase());
            if (!npmMatches.isEmpty()) {
                results.addAll(npmMatches);
                System.out.println("  Heuristic: npm package '" + npmName + "' matched");
            }
            break;

        case "PYPI":
            List<LocalCve> pypiMatches = localCveRepository
                .findByCpeProduct(fullName.toLowerCase());
            if (!pypiMatches.isEmpty()) {
                results.addAll(pypiMatches);
                System.out.println("  Heuristic: pypi package '" + fullName + "' matched");
            }
            break;
        }

        return results;
    }

    /**
     * LEVEL 3: Fuzzy Fallback Uses partial matching and the calculateMatchScore
     * scoring system
     */
    private List<LocalCve> lookupByFuzzyFallback(
        String fullName,
        String ecosystem,
        String version
    ) {
        String searchKey = extractSearchKey(fullName, ecosystem);
        List<LocalCve> results = new ArrayList<>();

        System.out.println("  Fuzzy fallback for: '" + searchKey + "'");

        // Get ALL potential CVEs by partial matching
        List<LocalCve> candidates = new ArrayList<>();

        // Strategy 1: Contains match on product
        candidates.addAll(localCveRepository.findByCpeProductContaining(searchKey.toLowerCase()));

        // Strategy 2: Match by product or vendor
        candidates
            .addAll(localCveRepository.findByProductOrVendorContaining(searchKey.toLowerCase()));

        // Strategy 3: For multi-part names, try each part
        if (searchKey.contains("-") || searchKey.contains("_")) {
            String[] parts = searchKey.split("[-_]");
            for (String part : parts) {
                if (part.length() > 3) {
                    candidates
                        .addAll(localCveRepository.findByCpeProductContaining(part.toLowerCase()));
                }
            }
        }

        if (candidates.isEmpty()) {
            return results;
        }

        // Deduplicate by CVE ID
        candidates = candidates.stream()
            .collect(Collectors.toMap(LocalCve::getCveId, c -> c, (c1, c2) -> c1))
            .values()
            .stream()
            .collect(Collectors.toList());

        // Score each candidate and keep only those with positive score
        for (LocalCve cve : candidates) {
            int score = calculateMatchScore(cve, searchKey, ecosystem, version);
            if (score > 0) {
                results.add(cve);
                System.out.println(
                    "    Scored " + score + ": " + cve.getCveId() +
                        " (product: " + cve.getCpeProduct() + ")"
                );
            }
        }

        System.out.println("    Fuzzy matches after scoring: " + results.size());
        return results;
    }

    /**
     * Group CVEs by ID and find the best matching CPE for each using scoring
     */
    private Map<String, Optional<LocalCve>> groupAndFindBestMatch(
        List<LocalCve> allCves,
        String fullName,
        String ecosystem,
        String version
    ) {

        String searchKey = extractSearchKey(fullName, ecosystem);

        Map<String, List<LocalCve>> groupedByCveId = allCves.stream()
            .collect(Collectors.groupingBy(LocalCve::getCveId));

        Map<String, Optional<LocalCve>> bestMatches = new HashMap<>();

        for (Map.Entry<String, List<LocalCve>> entry : groupedByCveId.entrySet()) {
            String cveId = entry.getKey();
            List<LocalCve> cveEntries = entry.getValue();

            // Score and pick the best matching CPE for this CVE
            Optional<LocalCve> bestMatch = cveEntries.stream()
                .max((c1, c2) -> {
                    int score1 = calculateMatchScore(c1, searchKey, ecosystem, version);
                    int score2 = calculateMatchScore(c2, searchKey, ecosystem, version);
                    return Integer.compare(score1, score2);
                });

            bestMatches.put(cveId, bestMatch);
        }

        return bestMatches;
    }

    /**
     * Calculate how well a CVE entry matches our dependency Higher score = better
     * match This is the FALLBACK scoring system used in Level 3
     */
    private int calculateMatchScore(
        LocalCve cve,
        String searchKey,
        String ecosystem,
        String version
    ) {
        int score = 0;
        String cveProduct = cve.getCpeProduct().toLowerCase();
        String targetProduct = searchKey.toLowerCase();

        // 1. Exact vs. Fuzzy Product Matching
        if (cveProduct.equals(targetProduct)) {
            score += 100;
        } else if (targetProduct.replace("-", "_").equals(cveProduct)) {
            score += 90;
        } else if (cveProduct.contains(targetProduct) || targetProduct.contains(cveProduct)) {
            score += 40;
        }

        // 2. Vendor matching for Maven
        if ("MAVEN".equalsIgnoreCase(ecosystem)) {
            String vendor = extractVendorFromGroupIdForScore(searchKey);
            if (!vendor.isEmpty() && cve.getCpeVendor().toLowerCase().contains(vendor)) {
                score += 50;
            }
        }

        // 3. NPM scope matching
        if ("NPM".equalsIgnoreCase(ecosystem) && searchKey.startsWith("@")) {
            String scope = searchKey.split("/")[0].substring(1);
            if (cve.getCpeVendor().toLowerCase().equals(scope)) {
                score += 50;
            }
        }

        // 4. Bonus if version falls within range
        if (isVersionAffected(cve, version)) {
            score += 20;
        }

        return score;
    }

    /**
     * Extract search key from dependency name
     */
    private String extractSearchKey(String fullName, String ecosystem) {
        switch (ecosystem.toUpperCase()) {
        case "MAVEN":
            if (fullName.contains(":")) {
                return fullName.split(":")[1];
            }
            return fullName;
        case "NPM":
            if (fullName.startsWith("@")) {
                String unscoped = fullName.substring(1);
                if (unscoped.contains("/")) {
                    return unscoped.split("/")[1];
                }
                return unscoped;
            }
            return fullName;
        default:
            return fullName;
        }
    }

    /**
     * Extract vendor from Maven groupId for scoring
     */
    private String extractVendorFromGroupIdForScore(String fullName) {
        if (fullName.contains(":")) {
            String groupId = fullName.split(":")[0];
            String[] parts = groupId.split("\\.");
            if (parts.length > 0) {
                String lastPart = parts[parts.length - 1];
                if (lastPart.equals("log4j") && groupId.contains("apache")) {
                    return "apache";
                }
                if (lastPart.equals("springframework") || lastPart.equals("spring")) {
                    return "spring";
                }
                return lastPart;
            }
        }
        return "";
    }

    private String extractVendorFromGroupId(String groupId) {
        String[] parts = groupId.split("\\.");
        if (parts.length == 0)
            return "";
        String lastPart = parts[parts.length - 1];
        if (lastPart.equals("log4j") && groupId.contains("apache")) {
            return "apache";
        }
        if (lastPart.equals("springframework") || lastPart.equals("spring")) {
            return "spring";
        }
        return lastPart;
    }

    /**
     * Check if a specific version is affected by this CVE
     */
    private boolean isVersionAffected(LocalCve cve, String currentVersion) {
        try {
            ComparableVersion current = new ComparableVersion(currentVersion);

            if (cve.getVersionStartIncluding() == null &&
                cve.getVersionStartExcluding() == null &&
                cve.getVersionEndIncluding() == null &&
                cve.getVersionEndExcluding() == null) {
                return true;
            }

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

            if (cve.getVersionEndIncluding() != null) {
                ComparableVersion endInc = new ComparableVersion(cve.getVersionEndIncluding());
                if (current.compareTo(endInc) > 0)
                    return false;
            }

            if (cve.getVersionEndExcluding() != null) {
                ComparableVersion endExc = new ComparableVersion(cve.getVersionEndExcluding());
                if (current.compareTo(endExc) >= 0)
                    return false;
            }

            return true;
        } catch (Exception e) {
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