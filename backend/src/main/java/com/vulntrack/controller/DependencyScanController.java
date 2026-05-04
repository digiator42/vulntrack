package com.vulntrack.controller;

import java.util.ArrayList;
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
import com.vulntrack.dto.DependencyScanResult;
import com.vulntrack.dto.ParsedDependency;
import com.vulntrack.service.DependencyParserService;
import com.vulntrack.service.NvdClientService;
import com.vulntrack.service.NvdClientService2;
import com.vulntrack.service.VulnerabilityIssueService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/admin/dependency/scan")
@RequiredArgsConstructor
@Slf4j
public class DependencyScanController {
    private final DependencyParserService dependencyParserService;
    private final NvdClientService2 nvdClientService;
    private final VulnerabilityIssueService vulnerabilityIssueService;

    @PostMapping("/parse")
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
    public ResponseEntity<Map<String, Object>> scanDependenciesAgainstNvd(
        @RequestParam("file") MultipartFile file
    ) {
        try {
            String fileName = file.getOriginalFilename();

            // First parse the manifest
            List<ParsedDependency> deps = dependencyParserService
                .parse(file.getInputStream(), fileName);

            // Then check against NVD
            Map<ParsedDependency, List<CveVulnerability>> vulnerabilities = nvdClientService
                .checkDependencies(deps, fileName);

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

    @PostMapping("/scan")
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_SECURITY_ADMIN')")
    public ResponseEntity<DependencyScanResult> scanAndCreateIssues(
        @RequestParam("file") MultipartFile file,
        @RequestParam(defaultValue = "false") boolean autoCreateIssues
    ) {

        try {
            String fileName = file.getOriginalFilename();

            // Parse dependencies
            List<ParsedDependency> dependencies = dependencyParserService
                .parse(file.getInputStream(), fileName);

            // Check against NVD
            Map<ParsedDependency, List<CveVulnerability>> vulnerabilities = nvdClientService
                .checkDependencies(dependencies, fileName);

            // Create issues for vulnerabilities
            DependencyScanResult result = DependencyScanResult.builder()
                .manifestFileName(fileName)
                .totalDependencies(dependencies.size())
                .vulnerableDependencies(vulnerabilities.size())
                .vulnerabilities(new ArrayList<>())
                .build();

            for (Map.Entry<ParsedDependency, List<CveVulnerability>> entry : vulnerabilities
                .entrySet()) {
                ParsedDependency dep = entry.getKey();
                List<CveVulnerability> cves = entry.getValue();

                List<Long> issueIds = new ArrayList<>();
                if (autoCreateIssues && !cves.isEmpty()) {
                    issueIds = vulnerabilityIssueService
                        .createIssuesForVulnerabilities(dep, cves, fileName);
                }

                result.vulnerabilities().add(
                    DependencyScanResult.VulnerableDependency.builder()
                        .dependency(dep)
                        .cves(cves)
                        .createdIssueIds(issueIds)
                        .build()
                );
            }

            log.info(
                "Scan completed for {}: {} dependencies, {} vulnerable, {} issues created",
                fileName, dependencies.size(), vulnerabilities.size(),
                result.vulnerabilities().stream().mapToInt(v -> v.createdIssueIds().size())
                    .sum()
            );

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Error scanning manifest", e);
            return ResponseEntity.badRequest().body(null);
        }
    }

}
