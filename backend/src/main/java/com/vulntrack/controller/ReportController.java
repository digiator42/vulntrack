package com.vulntrack.controller;

import com.vulntrack.service.ReportService;
import lombok.RequiredArgsConstructor;

import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/report")
public class ReportController {
    private final ReportService reportService;

    @GetMapping("/pdf")
    public ResponseEntity<byte[]> downloadReport() throws Exception {
        // Security is handled inside generateRoleBasedReport()
        byte[] pdf = reportService.generateRoleBasedReport();

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=vulntrack-report.pdf")
            .contentType(MediaType.APPLICATION_PDF)
            .body(pdf);
    }

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSummary() {
        Map<String, Object> summary = reportService.getJsonSummary();
        return ResponseEntity.ok(summary);
    }
}