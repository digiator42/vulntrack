package com.vulntrack.service;

import com.vulntrack.dto.ParsedDependency;
import com.vulntrack.model.PackageCpeMapping;
import com.vulntrack.repository.PackageCpeMappingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Resolves a raw dependency name to a canonical CPE vendor + product pair.
 *
 * All matching is data-driven against the package_cpe_mapping table. There are
 * NO hardcoded vendor names, product names, or ecosystem switch-cases.
 *
 * Pipeline (stops at first hit): L1 Exact product name lookup (multi-row safe —
 * picks best by vendor/scope alignment) L2 Vendor-scoped containment (vendor
 * candidates derived from scope tokens via bulk DB query) L3 Progressive suffix
 * stripping → retry L1 + L2 L4 Token-by-token fallback (each meaningful word in
 * the name) L5 Clean name pass-through (no mapping found; caller will do fuzzy
 * search)
 *
 * Required repository methods (add to PackageCpeMappingRepository):
 * ───────────────────────────────────────────────────────────────── // Returns
 * ALL rows matching the product name — NEVER throws on multiple
 * results @Query("SELECT m FROM PackageCpeMapping m WHERE LOWER(m.product) =
 * LOWER(:product)") List<PackageCpeMapping>
 * findAllByProductCaseInsensitive(@Param("product") String product);
 *
 * // Bulk vendor lookup — replaces N individual findByVendorCaseInsensitive
 * calls @Query("SELECT m FROM PackageCpeMapping m WHERE LOWER(m.vendor) IN
 * :vendors") List<PackageCpeMapping>
 * findByVendorInCaseInsensitive(@Param("vendors") List<String> vendors);
 *
 * // These existing methods are still used: List<PackageCpeMapping>
 * findByVendorCaseInsensitive(String vendor); // L2 fallback
 * List<PackageCpeMapping> findByCpeProduct(String product); //
 * NvdClientService2
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DependencyNormalizer {

    private final PackageCpeMappingRepository mappingRepository;

    // ── public entry point ────────────────────────────────────────────────────

    public NormalizationResult normalize(ParsedDependency dep) {
        // Decompose the raw dependency name into searchable tokens.
        NameTokens tokens = decompose(dep);

        log.debug("Normalizing: {} -> tokens={}", dep.name(), tokens);
        System.out.println("======>> " + dep.name() + tokens);

        // L1: exact product match on the primary artifact token
        // Pass scope tokens so that when multiple vendors share the product name,
        // we pick the one most aligned with the groupId / NPM scope.
        Optional<PackageCpeMapping> hit = exactProduct(tokens.artifact(), tokens.scopeTokens());
        if (hit.isPresent())
            return buildResult(hit.get(), dep.name(), "L1:exact");

        // L2: vendor-scoped lookup — vendor candidates come from the DB too
        hit = vendorScoped(tokens);
        if (hit.isPresent())
            return buildResult(hit.get(), dep.name(), "L2:vendor-scoped");

        // L3: strip suffixes progressively, retry L1 + L2 each time
        List<String> stripped = stripSuffixes(tokens.artifact());
        for (String candidate : stripped) {
            hit = exactProduct(candidate, tokens.scopeTokens());
            if (hit.isPresent())
                return buildResult(hit.get(), dep.name(), "L3:suffix-stripped:" + candidate);

            hit = vendorScoped(new NameTokens(candidate, tokens.scopeTokens(), tokens.ecosystem()));
            if (hit.isPresent())
                return buildResult(hit.get(), dep.name(), "L3b:vendor+stripped:" + candidate);
        }

        // L4: token-by-token — ONLY accept if the matched vendor is plausible for this
        // dependency. A bare token like "starter" exists in CPE under vendor "siemens",
        // which has zero relationship to groupId "org.springdoc". Without this guard,
        // any generic word that happens to be a CPE product name becomes a false
        // positive.
        //
        // Plausibility rule: the matched vendor must share a substring with at least
        // one
        // scope token (groupId segments, NPM scope) OR with the artifact name itself.
        // "siemens" shares nothing with ["org","springdoc"] or "springdoc-openapi-..."
        // → rejected.
        // "apache" shares with ["org","apache","commons"] → accepted.
        for (String part : meaningfulParts(tokens.artifact())) {
            hit = exactProduct(part, tokens.scopeTokens());
            if (hit.isPresent() && isVendorPlausible(hit.get().getVendor(), tokens)) {
                System.out.println("---> vendor is ***plausible***");
                return buildResult(hit.get(), dep.name(), "L4:token:" + part);
            }
            if (hit.isPresent()) {
                log.debug(
                    "L4 rejected '{}' → {}:{} — vendor not plausible for scope {}",
                    part, hit.get().getVendor(), hit.get().getProduct(), tokens.scopeTokens()
                );
            }
        }

        // L5: pass-through — no mapping found; return clean name for fuzzy fallback
        String clean = tokens.artifact();
        log.debug("No mapping for '{}', passing through as '{}'", dep.name(), clean);
        return null;
        // return new NormalizationResult(
        //     clean, clean, dep.name(), dep.ecosystem(), "L5:pass-through"
        // );
    }

    // ── decomposition ─────────────────────────────────────────────────────────

    /**
     * Splits a raw dependency name into: - artifact : the primary searchable
     * product token (e.g. "log4j-core" → "log4j-core") - scopeTokens : ordered list
     * of namespace segments that hint at the vendor (e.g. groupId parts, NPM scope,
     * PyPI namespace)
     *
     * This is the ONLY place that knows about format conventions (Maven colon,
     * NPM @scope/pkg, etc.) — and even here no vendor names are hardcoded.
     */
    private NameTokens decompose(ParsedDependency dep) {
        String raw = dep.name().trim();
        String eco = dep.ecosystem();

        // Maven: "org.apache.logging.log4j:log4j-core"
        if (raw.contains(":")) {
            String[] parts = raw.split(":", 2);
            List<String> scopeTokens = Arrays.asList(parts[0].split("\\."));
            return new NameTokens(parts[1].toLowerCase(), scopeTokens, eco);
        }

        // NPM scoped: "@angular/core"
        if (raw.startsWith("@") && raw.contains("/")) {
            String noAt = raw.substring(1);
            String scope = noAt.split("/")[0];
            String pkg = noAt.split("/", 2)[1];
            return new NameTokens(pkg.toLowerCase(), List.of(scope), eco);
        }

        // Everything else: PyPI, NuGet, plain NPM, Gradle without colon, etc.
        return new NameTokens(raw.toLowerCase(), List.of(), eco);
    }

    // ── L1: exact product ────────────────────────────────────────────────────

    /**
     * Looks up all mappings whose product matches the name (case-insensitive). When
     * multiple vendors share the same product name (e.g. "commons-csv" exists under
     * "apache" and two others), we prefer the row whose vendor appears in the scope
     * tokens, then fall back to highest usage count, then first row.
     *
     * Returns Optional.empty() only when the product truly does not exist in the
     * table. Never throws IncorrectResultSizeDataAccessException.
     */
    private Optional<PackageCpeMapping> exactProduct(String name) {
        return exactProduct(name, List.of());
    }

    private Optional<PackageCpeMapping> exactProduct(String name, List<String> scopeTokens) {
        // Repository now returns List — no unique-result constraint
        List<PackageCpeMapping> rows = mappingRepository.findAllByProductCaseInsensitive(name);

        System.out.println("=====> " + rows);

        if (rows.isEmpty())
            return Optional.empty();
        if (rows.size() == 1)
            return Optional.of(rows.get(0));

        // Multiple rows: prefer the one whose vendor appears in the scope tokens
        Set<String> scopeLower = scopeTokens.stream()
            .map(String::toLowerCase)
            .collect(Collectors.toSet());

        return rows.stream()
            // vendor token appears anywhere in the scope (e.g. "apache" in groupId)
            .filter(
                m -> scopeLower.stream().anyMatch(
                    s -> s.contains(m.getVendor().toLowerCase())
                        || m.getVendor().toLowerCase().contains(s)
                )
            )
            .findFirst()
            // No scope hint matched → highest usage count wins (most battle-tested mapping)
            .or(() -> rows.stream().max(Comparator.comparingInt(PackageCpeMapping::getUsageCount)));
    }

    // ── L2: vendor-scoped ────────────────────────────────────────────────────

    /**
     * Derives vendor candidates entirely from the DB: 1. Each scope token is looked
     * up as a vendor in the mapping table. 2. Additionally, the artifact name
     * itself is tried as a vendor (handles cases where the package name IS the
     * vendor, e.g. "struts").
     *
     * For each vendor candidate we then search for a product that contains the
     * artifact token or vice-versa.
     */
    private Optional<PackageCpeMapping> vendorScoped(NameTokens tokens) {
        // Single query to get all mappings for candidate vendor tokens at once,
        // avoiding N+1 existence-check queries.
        List<String> candidateTokens = buildRawCandidateTokens(tokens);
        if (candidateTokens.isEmpty())
            return Optional.empty();

        List<PackageCpeMapping> rows = mappingRepository
            .findByVendorInCaseInsensitive(candidateTokens);

        // Among the rows returned, find one whose product matches the artifact
        return rows.stream()
            .filter(m -> containsEither(m.getProduct(), tokens.artifact()))
            // Prefer the vendor token that appears earliest in scopeTokens (most specific
            // hint)
            .min(Comparator.comparingInt(m -> indexOfToken(tokens.scopeTokens(), m.getVendor())));
    }

    /**
     * Collects raw candidate strings from scope tokens + artifact parts. Does NOT
     * query the DB here — filtering happens via the bulk IN query in
     * vendorScoped().
     */
    private List<String> buildRawCandidateTokens(NameTokens tokens) {
        List<String> candidates = new ArrayList<>();
        tokens.scopeTokens().stream()
            .filter(t -> t.length() > 2)
            .map(String::toLowerCase)
            .forEach(candidates::add);
        candidates.add(tokens.artifact());
        candidates.addAll(meaningfulParts(tokens.artifact()));
        return candidates.stream().distinct().collect(Collectors.toList());
    }

    /**
     * Returns the index of the first scope token that contains the vendor string,
     * or MAX_INT.
     */
    private int indexOfToken(List<String> scopeTokens, String vendor) {
        String v = vendor.toLowerCase();
        for (int i = 0; i < scopeTokens.size(); i++) {
            if (scopeTokens.get(i).toLowerCase().contains(v))
                return i;
        }
        return Integer.MAX_VALUE;
    }

    // ── L3: suffix stripping ─────────────────────────────────────────────────

    /**
     * Returns progressively shorter versions of the name by stripping the rightmost
     * dash/underscore-delimited segment one at a time.
     *
     * Example: "log4j-core-extra" → ["log4j-core", "log4j"]
     *
     * Candidates shorter than MIN_CANDIDATE_LENGTH are dropped — single generic
     * words like "starter", "web", "core" match unrelated CPE products and cause
     * false positives (e.g. "starter" → Siemens STARTER software).
     */
    private static final int MIN_CANDIDATE_LENGTH = 6;

    private List<String> stripSuffixes(String name) {
        List<String> results = new ArrayList<>();
        String current = name;
        while (true) {
            int lastDash = current.lastIndexOf('-');
            int lastUnder = current.lastIndexOf('_');
            int cut = Math.max(lastDash, lastUnder);
            if (cut <= 0)
                break;
            current = current.substring(0, cut);
            if (current.length() >= MIN_CANDIDATE_LENGTH) {
                results.add(current);
            }
        }
        return results;
    }

    // ── L4: token parts ──────────────────────────────────────────────────────

    /**
     * Splits on [-_.] and keeps only segments at or above MIN_CANDIDATE_LENGTH.
     * This prevents generic tokens like "web", "core", "api", "starter" from
     * matching completely unrelated CPE products.
     */
    private List<String> meaningfulParts(String name) {
        return Arrays.stream(name.split("[-_.]"))
            .filter(p -> p.length() >= MIN_CANDIDATE_LENGTH)
            .distinct()
            .collect(Collectors.toList());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    /**
     * True only when one string contains the other AND the overlap is substantial
     * relative to the original artifact name.
     *
     * This is the false-positive guard: "starter" contains "starter" from
     * "springdoc-openapi-starter-webmvc-ui", but the overlap ratio against the full
     * artifact name is tiny, so the match is rejected.
     *
     * Threshold: the matched product must cover at least 40% of the artifact's
     * length (or vice-versa) to be considered a real match.
     */
    private static final double MIN_OVERLAP_RATIO = 0.80;

    private boolean containsEither(String product, String artifact) {
        String p = product.toLowerCase();
        String a = artifact.toLowerCase();
        if (!p.contains(a) && !a.contains(p))
            return false;

        // Overlap ratio: shorter / longer must be ≥ threshold
        int shorter = Math.min(p.length(), a.length());
        int longer = Math.max(p.length(), a.length());
        double ratio = (double) shorter / longer;
        return ratio >= MIN_OVERLAP_RATIO;
    }

    /**
     * Returns true when the CPE vendor has a plausible relationship to the
     * dependency's namespace — i.e. the vendor string and at least one scope token
     * share a common substring, OR the vendor appears in the artifact name.
     *
     * This is the primary guard against cross-domain false positives such as:
     * artifact: "springdoc-openapi-starter-webmvc-ui" (groupId: org.springdoc)
     * matched vendor: "siemens" → "siemens" shares nothing with ["org","springdoc"]
     * or the artifact → REJECTED
     *
     * artifact: "commons-csv" (groupId: org.apache.commons) matched vendor:
     * "apache" → "apache" is contained in scope token "apache" → ACCEPTED
     *
     * Used at L4 (token-by-token) where vendor context is weakest. L1/L2/L3 already
     * use scope-preferred selection, so the risk there is lower, but this method
     * can be applied to them too if false positives appear.
     */
    private boolean isVendorPlausible(String vendor, NameTokens tokens) {
        String v = vendor.toLowerCase();

        System.out.println("=======>>> " + vendor);

        // Check against each scope token
        for (String scopeToken : tokens.scopeTokens()) {
            String s = scopeToken.toLowerCase();
            if (s.contains(v) || v.contains(s)) {
                System.out.println("====> YES " + s + " " + v);
                return true;
            }
        }

        // Check against the artifact name itself
        return tokens.artifact().contains(v);
    }

    private NormalizationResult buildResult(PackageCpeMapping m, String original, String reason) {
        m.setUsageCount(m.getUsageCount() + 1);
        mappingRepository.save(m);
        log.info("✓ '{}' → {}:{} [{}]", original, m.getVendor(), m.getProduct(), reason);
        return new NormalizationResult(
            m.getProduct(), m.getVendor(), original, m.getEcosystem(), reason
        );
    }

    // ── inner types ───────────────────────────────────────────────────────────

    /**
     * Holds the decomposed parts of a raw dependency name.
     *
     * @param artifact    primary token to match against CPE product (lowercase)
     * @param scopeTokens ordered namespace hints (groupId segments, NPM scope,
     *                    etc.)
     * @param ecosystem   original ecosystem string
     */
    public record NameTokens(
        String artifact,
        List<String> scopeTokens,
        String ecosystem
    ) {
    }

    public record NormalizationResult(
        String cpeProduct,
        String cpeVendor,
        String originalName,
        String mappedEcosystem,
        String matchReason
    ) {
    }
}