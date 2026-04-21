package com.vulntrack.controller;

import com.vulntrack.model.CweEntry;
import com.vulntrack.service.CweService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cwe")
@RequiredArgsConstructor
public class CweController {

    private final CweService cweService;

    @GetMapping
    public ResponseEntity<List<CweEntry>> listAllCwes() {
        return ResponseEntity.ok(cweService.getAllCwes());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CweEntry> getCweDetails(@PathVariable String id) {
        CweEntry entry = cweService.getCweById(id);
        if (entry == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(entry);
    }
}