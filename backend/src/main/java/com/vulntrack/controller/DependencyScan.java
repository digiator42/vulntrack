package com.vulntrack.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.vulntrack.dto.CveVulnerability;
import com.vulntrack.dto.NvdCveResponse;
import com.vulntrack.dto.ParsedDependency;
import com.vulntrack.service.DependencyParserService;
import com.vulntrack.service.NvdClientService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/dependency/scan")
@RequiredArgsConstructor
public class DependencyScan {
    private final DependencyParserService dependencyParserService;
    private final NvdClientService nvdClientService;

    @PostMapping("/parse")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<List<ParsedDependency>> parseManifest(
        @RequestParam("file") MultipartFile file
    ) {
        try {
            System.out.println("======> " + file.getOriginalFilename());
            List<ParsedDependency> deps = dependencyParserService
                .parse(file.getInputStream(), file.getOriginalFilename());
            return ResponseEntity.ok(deps);
        } catch (Exception e) {
            e.printStackTrace(); // Prints to console
            System.err.println("Error parsing file: " + e.getMessage());
            return ResponseEntity.badRequest().body(null);
        }
    }

    @PostMapping("/nvd")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Map<String, Object>> scanDependenciesAgainstNvd(
        @RequestParam("file") MultipartFile file
    ) {
        try {
            // First parse the manifest
            List<ParsedDependency> deps = dependencyParserService
                .parse(file.getInputStream(), file.getOriginalFilename());

            // Then check against NVD
            Map<ParsedDependency, List<CveVulnerability>> vulnerabilities = nvdClientService
                .checkDependencies(deps);

            Map<String, Object> response = new HashMap<>();
            response.put("totalDependencies", deps.size());
            response.put("vulnerableDependencies", vulnerabilities.size());
            response.put("results", vulnerabilities);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            // log.error("Error scanning dependencies", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

}
