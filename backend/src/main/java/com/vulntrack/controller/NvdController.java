package com.vulntrack.controller;

import com.vulntrack.dto.CveResult;
import com.vulntrack.service.NvdCveByCpeClientService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/nvd")
@RequiredArgsConstructor
public class NvdController {

    private final NvdCveByCpeClientService nvdCveClientService;

    @GetMapping("/cves")
    public ResponseEntity<List<CveResult>> getCvesByCpe(@RequestParam String cpeName) {
        if (cpeName == null || cpeName.isEmpty()) {
            return ResponseEntity.badRequest().body(null);
        }

        List<CveResult> results = nvdCveClientService.fetchCvesByCpe(cpeName);
        return ResponseEntity.ok(results);
    }
}