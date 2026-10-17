package com.vulntrack.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vulntrack.dto.ParsedDependency;
import com.vulntrack.entity.OsvAdvisory;
import com.vulntrack.repository.OsvAdvisoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Client for the OSV.dev API (https://google.github.io/osv.dev/api/).
 *
 * <p>OSV is ecosystem-native: it answers "is &lt;package&gt;@&lt;version&gt;
 * vulnerable" with no CPE guessing, which is exactly where the local NVD
 * path is lossy. The trade-off is that OSV only covers published package
 * ecosystems, so it complements rather than replaces the NVD cache.
 *
 * <p>Two calls, not N: {@code POST /v1/querybatch} accepts up to 1000
 * (package, version) pairs but returns only ids, so full records are fetched
 * once per unique id via {@code GET /v1/vulns/{id}} and then cached.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OsvClient {

    /** Max entries OSV accepts in a single querybatch. */
    private static final int BATCH_LIMIT = 1000;
    private static final int DETAIL_CONCURRENCY = 8;
    /** Advisories cached for this long before a refresh. */
    private static final long CACHE_MAX_AGE_HOURS = 24;

    private final OsvAdvisoryRepository advisoryRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final RestClient restClient = RestClient.builder()
        .requestFactory(defaultRequestFactory())
        .build();

    @Value("${osv.api.url:https://api.osv.dev/v1}")
    private String apiUrl = "https://api.osv.dev/v1";

    private static SimpleClientHttpRequestFactory defaultRequestFactory() {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(30));
        return factory;
    }

    // ── ecosystem + version mapping ───────────────────────────────────────────

    /**
     * Maps parser ecosystem labels onto OSV ecosystem names. GRADLE shares
     * Maven coordinates, so it maps to Maven.
     */
    public static Optional<String> toOsvEcosystem(String ecosystem) {
        if (ecosystem == null) return Optional.empty();
        return switch (ecosystem.trim().toUpperCase()) {
            case "MAVEN", "GRADLE" -> Optional.of("Maven");
            case "NPM" -> Optional.of("npm");
            case "PYPI", "PIP" -> Optional.of("PyPI");
            case "NUGET" -> Optional.of("NuGet");
            case "CARGO", "RUST", "RUSTIC" -> Optional.of("crates.io");
            case "GO", "GOMOD" -> Optional.of("Go");
            case "RUBY", "BUNDLER", "GEM" -> Optional.of("RubyGems");
            case "PACKAGIST", "COMPOSER" -> Optional.of("Packagist");
            default -> Optional.empty();
        };
    }

    /**
     * OSV needs a concrete released version, so ranges are reduced to the
     * nearest concrete version we can actually test: "^1.2.3" -> "1.2.3",
     * ">=1.0.0 &lt;2.0.0" -> "1.0.0".
     */
    static String normalizeVersion(String raw) {
        if (raw == null) return null;
        String value = raw.trim().replaceFirst("^[\\^~><=!*\\s]+", "");
        String firstNumeric = Arrays.stream(value.split("\\s+"))
            .filter(token -> token.matches(".*\\d.*"))
            .findFirst()
            .orElse(value);
        firstNumeric = firstNumeric.replaceAll("[\\[\\](){},;].*$", "").trim();
        return firstNumeric.isEmpty() ? null : firstNumeric;
    }

    // ── public API ────────────────────────────────────────────────────────────

    /**
     * Returns the OSV advisory ids affecting each supplied dependency.
     * Unsupported ecosystems and unresolvable versions are skipped; any
     * transport failure degrades to an empty result so a scan still runs.
     */
    public Map<ParsedDependency, List<String>> findAdvisoryIds(List<ParsedDependency> deps) {
        Map<ParsedDependency, List<String>> found = new LinkedHashMap<>();

        List<ParsedDependency> queryable = deps.stream()
            .filter(dep -> toOsvEcosystem(dep.ecosystem()).isPresent())
            .filter(dep -> normalizeVersion(dep.version()) != null)
            .toList();

        for (int start = 0; start < queryable.size(); start += BATCH_LIMIT) {
            List<ParsedDependency> slice =
                queryable.subList(start, Math.min(queryable.size(), start + BATCH_LIMIT));
            try {
                found.putAll(queryBatch(slice));
            } catch (Exception e) {
                log.warn("OSV querybatch failed for slice starting at {}: {}", start, e.getMessage());
            }
        }

        return found;
    }

    private Map<ParsedDependency, List<String>> queryBatch(List<ParsedDependency> deps)
        throws com.fasterxml.jackson.core.JsonProcessingException {
        StringBuilder queries = new StringBuilder();
        List<ParsedDependency> queried = new ArrayList<>();

        for (ParsedDependency dep : deps) {
            String version = normalizeVersion(dep.version());
            String ecosystem = toOsvEcosystem(dep.ecosystem()).orElse(null);
            if (ecosystem == null || version == null) continue;

            if (!queries.isEmpty()) queries.append(',');
            queries.append("{\"package\":{\"name\":")
                .append(asJson(dep.name()))
                .append(",\"ecosystem\":").append(asJson(ecosystem))
                .append("},\"version\":").append(asJson(version))
                .append('}');
            queried.add(dep);
        }

        if (queried.isEmpty()) return Map.of();

        String body = "{\"queries\":[" + queries + "]}";
        String response = restClient.post()
            .uri(apiUrl + "/querybatch")
            .body(body)
            .retrieve()
            .onStatus(HttpStatusCode::isError, (req, res) -> {
                throw new IllegalStateException("OSV querybatch HTTP " + res.getStatusCode());
            })
            .body(String.class);

        JsonNode results = objectMapper.readTree(response).path("results");
        Map<ParsedDependency, List<String>> out = new LinkedHashMap<>();

        for (int i = 0; i < queried.size(); i++) {
            List<String> ids = new ArrayList<>();
            results.path(i).path("vulns").forEach(v -> {
                String id = v.path("id").asText(null);
                if (id != null && !id.isBlank()) ids.add(id);
            });
            out.put(queried.get(i), ids);
        }
        return out;
    }

    /**
     * Returns full advisory records for the given ids, serving recent records
     * from the local cache and fetching only what is missing or stale.
     */
    public List<OsvAdvisory> getAdvisories(List<String> ids) {
        if (ids == null || ids.isEmpty()) return List.of();

        List<OsvAdvisory> resolved = new ArrayList<>();
        List<String> missing = new ArrayList<>();

        for (String id : new LinkedHashSet<>(ids)) {
            Optional<OsvAdvisory> cached = advisoryRepository.findById(id);
            if (cached.isPresent() && !isStale(cached.get())) {
                resolved.add(cached.get());
            } else {
                missing.add(id);
            }
        }

        if (!missing.isEmpty()) resolved.addAll(fetchAndCache(missing));
        return resolved;
    }

    private boolean isStale(OsvAdvisory cached) {
        return cached.getSyncedAt() == null
            || cached.getSyncedAt().isBefore(LocalDateTime.now().minusHours(CACHE_MAX_AGE_HOURS));
    }

    private List<OsvAdvisory> fetchAndCache(List<String> ids) {
        ExecutorService pool = Executors.newFixedThreadPool(DETAIL_CONCURRENCY);
        try {
            List<CompletableFuture<Optional<OsvAdvisory>>> tasks = ids.stream()
                .map(id -> CompletableFuture.supplyAsync(() -> fetchOne(id), pool))
                .toList();

            List<OsvAdvisory> fetched = tasks.stream()
                .map(CompletableFuture::join)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();

            if (!fetched.isEmpty()) {
                try {
                    advisoryRepository.saveAll(fetched);
                } catch (Exception e) {
                    log.warn("Could not cache OSV advisories: {}", e.getMessage());
                }
            }
            return fetched;
        } finally {
            pool.shutdown();
        }
    }

    private Optional<OsvAdvisory> fetchOne(String id) {
        try {
            String json = restClient.get()
                .uri(apiUrl + "/vulns/{id}", id)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw new IllegalStateException("HTTP " + res.getStatusCode());
                })
                .body(String.class);
            return Optional.of(parseAdvisory(json));
        } catch (Exception e) {
            log.debug("OSV detail fetch failed for {}: {}", id, e.getMessage());
            return Optional.empty();
        }
    }

    private OsvAdvisory parseAdvisory(String json) throws Exception {
        JsonNode node = objectMapper.readTree(json);
        JsonNode databaseSpecific = node.path("database_specific");

        String aliases = joinOrNull(node.path("aliases"));

        // CVSS: severity[] carries vectors, database_specific may carry a score
        String vector = null;
        for (JsonNode severity : node.path("severity")) {
            if (vector == null && severity.hasNonNull("score")) {
                vector = severity.path("score").asText();
            }
        }
        Double score = null;
        JsonNode cvss = databaseSpecific.path("cvss");
        if (vector == null && cvss.hasNonNull("score")) vector = cvss.path("score").asText(null);
        if (cvss.hasNonNull("score")) score = cvss.path("score").asDouble();
        else if (databaseSpecific.hasNonNull("cvss_score")) {
            score = databaseSpecific.path("cvss_score").asDouble();
        }

        String severity = databaseSpecific.hasNonNull("severity")
            ? databaseSpecific.path("severity").asText()
            : (score != null ? severityFromScore(score) : null);

        String cwes = joinOrNull(databaseSpecific.path("cwe_ids"));

        List<String> affectedPackages = new ArrayList<>();
        for (JsonNode affected : node.path("affected")) {
            String name = affected.path("package").path("name").asText("");
            if (!name.isBlank()) affectedPackages.add(name);
        }

        return OsvAdvisory.builder()
            .id(node.path("id").asText())
            .aliases(aliases)
            .summary(node.path("summary").asText(null))
            .details(node.path("details").asText(null))
            .severity(severity)
            .cvssVector(vector)
            .cvssScore(score)
            .cweIds(cwes)
            .affectedRefs(affectedPackages.isEmpty()
                ? null : objectMapper.writeValueAsString(affectedPackages))
            .publishedDate(parseDate(node.path("published").asText(null)))
            .modifiedDate(parseDate(node.path("modified").asText(null)))
            .rawJson(json)
            .syncedAt(LocalDateTime.now())
            .build();
    }

    private String joinOrNull(JsonNode array) {
        List<String> values = new ArrayList<>();
        array.forEach(node -> {
            String text = node.asText();
            if (text != null && !text.isBlank()) values.add(text);
        });
        return values.isEmpty() ? null : values.stream().distinct().collect(Collectors.joining(","));
    }

    private static String severityFromScore(double score) {
        if (score >= 9.0) return "CRITICAL";
        if (score >= 7.0) return "HIGH";
        if (score >= 4.0) return "MEDIUM";
        if (score > 0) return "LOW";
        return "NONE";
    }

    private static LocalDateTime parseDate(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return LocalDateTime.parse(value.replace("Z", "").split("\\.")[0]);
        } catch (Exception e) {
            return null;
        }
    }

    private static String asJson(String value) {
        String safe = value == null ? "" : value;
        return "\"" + safe.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}