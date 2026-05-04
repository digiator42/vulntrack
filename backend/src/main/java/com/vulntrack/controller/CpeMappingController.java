package com.vulntrack.controller;

import com.vulntrack.model.PackageCpeMapping;
import com.vulntrack.repository.PackageCpeMappingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/admin/cpe-mappings")
@RequiredArgsConstructor
public class CpeMappingController {

    private final PackageCpeMappingRepository mappingRepository;

    @GetMapping("/search")
    public ResponseEntity<List<PackageCpeMapping>> findByProduct(@RequestParam String product) {
        return ResponseEntity.ok(mappingRepository.findByProductCaseInsensitive(product));
    }

    @PostMapping
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<PackageCpeMapping> addMapping(@RequestBody PackageCpeMapping mapping) {
        mapping.setCreatedAt(LocalDateTime.now());
        mapping.setUsageCount(0);
        mapping.setEnabled(true);
        return ResponseEntity.ok(mappingRepository.save(mapping));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Void> deleteMapping(@PathVariable Long id) {
        mappingRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }
}