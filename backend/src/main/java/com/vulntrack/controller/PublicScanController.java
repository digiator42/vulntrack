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
            Map<ParsedDependency, List<CveVulnerability>> vulnerabilities = nvdClientService
                .checkDependencies(deps, fileName);

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
     * Flow: 1. Load all DB rows for the product (there may be multiple vendors). 2.
     * If EVERY row is already enriched (rawCpeName set), return immediately — no
     * NVD API call needed. 3. Otherwise call NVD to get all matching CPE names for
     * this keyword. 4. For each CPE returned by NVD, parse vendor+product and
     * either update the matching existing DB row or create a new one. 5. Any DB
     * rows that NVD had no match for keep their existing data unchanged. 6. Return
     * the full merged list so the user can pick the right CPE.
     */
    @GetMapping("/cpe/search")
    public ResponseEntity<?> searchCpe(@RequestParam String product) {

        List<PackageCpeMapping> existingRows = mappingRepository
            .findAllByProductCaseInsensitive(product);

        // ── Fast path: all rows already enriched ──────────────────────────────
        // Only skip the API if every known row has a rawCpeName — not just one.
        // A product like "spring_boot" may have one enriched pivotal row and one
        // unenriched vmware row; we still want to fill in the missing one.
        boolean allEnriched = !existingRows.isEmpty()
            && existingRows.stream().allMatch(m -> m.getRawCpeName() != null);

        if (allEnriched) {
            log.debug(
                "CPE search '{}': all {} rows already enriched, skipping NVD",
                product, existingRows.size()
            );
            return ResponseEntity.ok(existingRows);
        }

        // ── NVD enrichment path ───────────────────────────────────────────────
        List<String> nvdCpeNames = nvdCpeClient.searchCpeInNvd(product);

        if (nvdCpeNames.isEmpty()) {
            // NVD returned nothing — return whatever we have (may be empty)
            return existingRows.isEmpty()
                ? ResponseEntity.notFound().build()
                : ResponseEntity.ok(existingRows);
        }

        // Build a mutable map of existing rows keyed by vendor (lowercase) for O(1)
        // lookup
        Map<String, PackageCpeMapping> existingByVendor = existingRows.stream()
            .collect(
                Collectors.toMap(
                    m -> m.getVendor().toLowerCase(),
                    m -> m,
                    (a, b) -> a // keep first on duplicate vendor (shouldn't happen)
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
            // If NVD returns a vendor we don't recognise, we skip it — we never
            // create rows from API data, as the mapping table is human-curated.
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

        // Always return the full set of curated rows (enriched or not),
        // so the user sees every known vendor and can pick the right one.
        log.info(
            "CPE search '{}': {} NVD hits enriched {} curated rows, returning {} total",
            product, nvdCpeNames.size(), toSave.size(), existingRows.size()
        );

        return existingRows.isEmpty()
            ? ResponseEntity.notFound().build()
            : ResponseEntity.ok(existingRows);
    }
}