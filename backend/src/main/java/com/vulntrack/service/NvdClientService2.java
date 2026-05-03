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

/**
 * Checks a parsed dependency against the local NVD CVE database.
 *
 * Architecture (no hardcoded vendor/product/ecosystem names anywhere):
 *
 * Phase 1 – Authoritative : DependencyNormalizer resolves dep → CPE
 * vendor+product using the package_cpe_mapping table. Phase 2 – Fuzzy fallback
 * : If Phase 1 yields no CVEs, token-based partial search is run; results are
 * ranked by a generic scoring function that uses only string similarity and
 * version range checks.
 *
 * The scoring function has NO ecosystem-specific branches. The fuzzy fallback
 * has NO ecosystem-specific branches. All ecosystem-specific name decomposition
 * lives solely in DependencyNormalizer.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NvdClientService2 {

    private final LocalCveRepository localCveRepository;
    private final DependencyNormalizer dependencyNormalizer;

    // ── public API ────────────────────────────────────────────────────────────

    public List<CveVulnerability> checkDependency(ParsedDependency dep, String manifestFile) {
        String version = cleanVersion(dep.version());
        log.info("Checking {} @ {} [{}]", dep.name(), version, dep.ecosystem());

        // ── Phase 1: authoritative mapping ────────────────────────────────────
        DependencyNormalizer.NormalizationResult norm = dependencyNormalizer.normalize(dep);
        log.debug(
            "Normalized: {} → {}:{} via {}", dep.name(), norm.cpeVendor(), norm.cpeProduct(),
            norm.matchReason()
        );

        List<LocalCve> candidates = fetchByNormalized(norm);

        // ── Phase 2: fuzzy fallback ───────────────────────────────────────────
        if (candidates.isEmpty()) {
            log.debug("No authoritative hit for '{}', running fuzzy fallback", dep.name());
            candidates = fetchByFuzzy(norm.cpeProduct());
        }

        if (candidates.isEmpty()) {
            log.debug("No CVE candidates found for '{}'", dep.name());
            return List.of();
        }

        // ── Deduplicate, score, filter by version ─────────────────────────────
        List<CveVulnerability> results = deduplicateAndScore(candidates, norm.cpeProduct(), version)
            .entrySet().stream()
            .filter(e -> e.getValue().isPresent())
            .filter(e -> isVersionAffected(e.getValue().get(), version))
            .map(e -> toCveVulnerability(e.getValue().get()))
            .collect(Collectors.toList());

        log.info("→ {} vulnerabilities for {}", results.size(), dep.name());
        return results;
    }

    public Map<ParsedDependency, List<CveVulnerability>> checkDependencies(
        List<ParsedDependency> deps,
        String manifestFile
    ) {
        Map<ParsedDependency, List<CveVulnerability>> out = new LinkedHashMap<>();
        for (ParsedDependency dep : deps) {
            List<CveVulnerability> vulns = checkDependency(dep, manifestFile);
            if (!vulns.isEmpty())
                out.put(dep, vulns);
        }
        return out;
    }

    // ── Phase 1: authoritative lookup ─────────────────────────────────────────

    private List<LocalCve> fetchByNormalized(DependencyNormalizer.NormalizationResult norm) {
        // Try vendor + product first (most precise)
        List<LocalCve> hits = localCveRepository.findByCpeVendorAndCpeProduct(
            norm.cpeVendor().toLowerCase(),
            norm.cpeProduct().toLowerCase()
        );

        // If vendor resolved to the same thing as product (L5 pass-through),
        // fall back to product-only search.
        if (hits.isEmpty() && !norm.cpeVendor().equals(norm.cpeProduct())) {
            hits = localCveRepository.findByCpeProduct(norm.cpeProduct().toLowerCase());
        }

        return hits;
    }

    // ── Phase 2: fuzzy fallback ───────────────────────────────────────────────

    /**
     * No ecosystem switch-cases here. We search by partial product match and also
     * try each dash/underscore segment of the search key individually. Results are
     * de-duplicated then ranked by the generic score function.
     */
    private List<LocalCve> fetchByFuzzy(String searchKey) {
        Set<LocalCve> bag = new LinkedHashSet<>();

        // Partial product name match
        bag.addAll(localCveRepository.findByCpeProductContaining(searchKey.toLowerCase()));
        bag.addAll(localCveRepository.findByProductOrVendorContaining(searchKey.toLowerCase()));

        // Try each meaningful segment of the search key
        for (String part : meaningfulParts(searchKey)) {
            bag.addAll(localCveRepository.findByCpeProductContaining(part));
        }

        return new ArrayList<>(bag);
    }

    // ── scoring & deduplication ───────────────────────────────────────────────

    /**
     * Groups CVE rows by CVE-ID, picks the best-scoring row per group. The score
     * function is purely string-similarity + version-range based; it has no
     * knowledge of ecosystems.
     */
    private Map<String, Optional<LocalCve>> deduplicateAndScore(
        List<LocalCve> rows,
        String searchKey,
        String version
    ) {
        return rows.stream()
            .collect(Collectors.groupingBy(LocalCve::getCveId))
            .entrySet().stream()
            .collect(
                Collectors.toMap(
                    Map.Entry::getKey,
                    e -> e.getValue().stream()
                        .max(Comparator.comparingInt(c -> score(c, searchKey, version)))
                )
            );
    }

    /**
     * Generic scoring — no ecosystem logic.
     *
     * +100 exact product match + 90 match after normalising separators (- vs _) +
     * 40 one string contains the other + 20 version falls within the affected range
     */
    private int score(LocalCve cve, String searchKey, String version) {
        String cp = cve.getCpeProduct().toLowerCase();
        String sk = searchKey.toLowerCase();

        int s = 0;
        if (cp.equals(sk))
            s += 100;
        else if (cp.equals(sk.replace("-", "_")))
            s += 90;
        else if (cp.equals(sk.replace("_", "-")))
            s += 90;
        else if (cp.contains(sk) || sk.contains(cp))
            s += 40;

        if (isVersionAffected(cve, version))
            s += 20;

        return s;
    }

    // ── version range check ───────────────────────────────────────────────────

    private boolean isVersionAffected(LocalCve cve, String version) {
        if (version == null)
            return false;
        try {
            ComparableVersion v = new ComparableVersion(version);

            boolean hasRange = cve.getVersionStartIncluding() != null ||
                cve.getVersionStartExcluding() != null ||
                cve.getVersionEndIncluding() != null ||
                cve.getVersionEndExcluding() != null;

            if (!hasRange)
                return true; // no range = affects all versions

            if (cve.getVersionStartIncluding() != null &&
                v.compareTo(new ComparableVersion(cve.getVersionStartIncluding())) < 0)
                return false;

            if (cve.getVersionStartExcluding() != null &&
                v.compareTo(new ComparableVersion(cve.getVersionStartExcluding())) <= 0)
                return false;

            if (cve.getVersionEndIncluding() != null &&
                v.compareTo(new ComparableVersion(cve.getVersionEndIncluding())) > 0)
                return false;

            if (cve.getVersionEndExcluding() != null &&
                v.compareTo(new ComparableVersion(cve.getVersionEndExcluding())) >= 0)
                return false;

            return true;
        } catch (Exception e) {
            log.warn("Version comparison failed for '{}': {}", version, e.getMessage());
            return false;
        }
    }

    // ── utilities ─────────────────────────────────────────────────────────────

    /** Strips leading semver operators and takes the first numeric token. */
    private String cleanVersion(String raw) {
        if (raw == null)
            return null;
        String v = raw.replaceAll("^[\\^~><=!*]+", "").trim();
        // ">=1.0 <2.0" → take first numeric group
        return Arrays.stream(v.split("\\s+"))
            .filter(t -> t.matches("\\d.*"))
            .findFirst()
            .orElse(v);
    }

    /** Splits on [-_.] and keeps tokens longer than 3 chars (avoids noise). */
    private List<String> meaningfulParts(String name) {
        return Arrays.stream(name.split("[-_.]"))
            .map(String::toLowerCase)
            .filter(p -> p.length() > 3)
            .distinct()
            .collect(Collectors.toList());
    }

    private CveVulnerability toCveVulnerability(LocalCve cve) {
        return new CveVulnerability(
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
        );
    }
}