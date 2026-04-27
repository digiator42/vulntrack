package com.vulntrack.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.vulntrack.dto.ParsedDependency;
import com.vulntrack.service.DependencyParserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/dependency")
@RequiredArgsConstructor
public class DependencyScan {
    private final DependencyParserService dependencyParserService;

    @PostMapping("/scan/parse")
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

}
