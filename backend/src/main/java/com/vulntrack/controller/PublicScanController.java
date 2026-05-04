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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/public")
@RequiredArgsConstructor
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

    @GetMapping("/cpe/search")
    public ResponseEntity<?> searchCpe(@RequestParam String product) {

        // Fetch existing mapping or prepare for a new one
        List<PackageCpeMapping> mappings = mappingRepository.findByProductCaseInsensitive(product);

        // If we already have the CPE name cached, just return it
        if (!mappings.isEmpty() && mappings.get(0).getRawCpeName() != null) {
            return ResponseEntity.ok(mappings.get(0));
        }

        // Fallback: Search NVD CPE Dictionary API
        String nvdCpeName = nvdCpeClient.searchCpeInNvd(product);

        if (nvdCpeName != null) {
            String[] parts = nvdCpeName.split(":");
            if (parts.length >= 5) {
                // Update the existing/new object with NVD data
                if (mappings.get(0) != null) {
                    mappings.get(0).setRawCpeName(nvdCpeName);
                    mappingRepository.save(mappings.get(0));
                }
                return ResponseEntity.ok(mappings.get(0));
            }
        }

        return ResponseEntity.notFound().build();
    }
}