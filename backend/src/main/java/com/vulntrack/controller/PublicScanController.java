// Create PublicScanController.java
package com.vulntrack.controller;

import com.vulntrack.dto.ParsedDependency;
import com.vulntrack.dto.CveVulnerability;
import com.vulntrack.model.PackageCpeMapping;
import com.vulntrack.repository.PackageCpeMappingRepository;
import com.vulntrack.service.DependencyParserService;
import com.vulntrack.service.NvdClientService2;
import com.vulntrack.service.NvdCpeClientService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/public")
@RequiredArgsConstructor
@Slf4j
public class PublicScanController {

    private final DependencyParserService dependencyParserService;
    private final NvdClientService2 nvdClientService;
    private final com.vulntrack.service.HybridVulnerabilityService hybridScanner;
    private final PackageCpeMappingRepository mappingRepository;
    private final NvdCpeClientService nvdCpeClient;

    @PostMapping("/scan/parse")
    public ResponseEntity<List<ParsedDependency>> parseManifest(
        @RequestParam("file") MultipartFile file
    ) {
        try {
            List<ParsedDependency> deps = dependencyParserService
                .parse(file.getInputStream(), file.getOriginalFilename());
            return ResponseEntity.ok(deps);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/scan/scan")
    public ResponseEntity<Map<String, Object>> scanDependencies(
        @RequestParam("file") MultipartFile file
    ) {
        try {
            String fileName = file.getOriginalFilename();
            List<ParsedDependency> deps = dependencyParserService
                .parse(file.getInputStream(), fileName);
            Map<ParsedDependency, List<CveVulnerability>> vulnerabilities =
                hybridScanner.scan(deps, fileName);

            Map<String, Object> response = new HashMap<>();
            response.put("manifestFileName", fileName);
            response.put("totalDependencies", deps.size());
            response.put("vulnerableDependencies", vulnerabilities.size());
            response.put(
                "vulnerabilities", vulnerabilities.entrySet().stream()
                    .map(
                        entry -> Map.of(
                            "dependency", entry.getKey(),
                            "cves", entry.getValue()
                        )
                    )
                    .toList()
            );

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Search for CPE mappings by product name.
     *
     * Flow: 1. Exact match in curated table → enrich via NVD if needed, return
     * results. 2. No exact match → fuzzy LIKE search in the 64k table → return
     * suggestions. The caller should re-invoke with the chosen product name from
     * the suggestions. We never forward an ambiguous user keyword to NVD directly —
     * "spring" would return hundreds of unrelated CPEs. The curated table is the
     * disambiguation layer. 3. No fuzzy match either → 404.
     *
     * Response shape: { "type": "results", "results": [ ...PackageCpeMapping... ] }
     * <- exact hit { "type": "suggestions", "query": "...", "suggestions": [ ... ]
     * } <- needs refinement
     */
    @GetMapping("/cpe/search")
    public ResponseEntity<?> searchCpe(@RequestParam String product) {

        List<PackageCpeMapping> exactRows = mappingRepository
            .findAllByProductCaseInsensitive(product);

        // ── No exact match: return fuzzy suggestions from the 64k table ───────
        // The user typed "spring" but the canonical product is "spring_framework".
        // We let the user pick the right product name from our curated table first,
        // then they re-call this endpoint with the confirmed name.
        if (exactRows.isEmpty()) {
            List<PackageCpeMapping> suggestions = mappingRepository
                .findByProductContainingIgnoreCase(product);

            if (suggestions.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            log.debug(
                "CPE search '{}': no exact match, returning {} suggestions",
                product, suggestions.size()
            );

            return ResponseEntity.ok(
                Map.of(
                    "type", "suggestions",
                    "query", product,
                    "suggestions", suggestions.subList(0, Math.min(suggestions.size(), 100))
                )
            );
        }

        // ── Fast path: all rows already enriched ──────────────────────────────
        boolean allEnriched = exactRows.stream()
            .allMatch(m -> m.getRawCpeName() != null);

        if (allEnriched) {
            log.debug(
                "CPE search '{}': all {} rows already enriched, skipping NVD",
                product, exactRows.size()
            );
            return ResponseEntity.ok(Map.of("type", "results", "results", exactRows));
        }

        // ── NVD enrichment path ───────────────────────────────────────────────
        // We reach here with a confirmed canonical product name (exact DB match),
        // so NVD will return precise results rather than a broad keyword dump.
        List<String> nvdCpeNames = nvdCpeClient.searchCpeInNvd(product);

        if (nvdCpeNames.isEmpty()) {
            return ResponseEntity.ok(Map.of("type", "results", "results", exactRows));
        }

        Map<String, PackageCpeMapping> existingByVendor = exactRows.stream()
            .collect(
                Collectors.toMap(
                    m -> m.getVendor().toLowerCase(),
                    m -> m,
                    (a, b) -> a
                )
            );

        List<PackageCpeMapping> toSave = new ArrayList<>();

        for (String cpeName : nvdCpeNames) {
            // cpe:2.3:a:<vendor>:<product>:<version>:...
            String[] parts = cpeName.split(":");
            if (parts.length < 5) {
                log.warn("Unexpected CPE format, skipping: {}", cpeName);
                continue;
            }
            String nvdVendor = parts[3].toLowerCase();

            // Only enrich rows that already exist in the curated mapping table.
            // Never create rows from API data — the mapping table is human-curated.
            PackageCpeMapping existing = existingByVendor.get(nvdVendor);
            if (existing == null) {
                log.debug(
                    "CPE search '{}': NVD vendor '{}' has no curated row, skipping",
                    product, nvdVendor
                );
                continue;
            }

            // Preserve manual overrides — only write if rawCpeName is not yet set
            if (existing.getRawCpeName() == null) {
                existing.setRawCpeName(cpeName);
                toSave.add(existing);
            }
        }

        if (!toSave.isEmpty()) {
            mappingRepository.saveAll(toSave);
        }

        log.info(
            "CPE search '{}': {} NVD hits enriched {} curated rows, returning {} total",
            product, nvdCpeNames.size(), toSave.size(), exactRows.size()
        );

        return ResponseEntity.ok(Map.of("type", "results", "results", exactRows));
    }   
}