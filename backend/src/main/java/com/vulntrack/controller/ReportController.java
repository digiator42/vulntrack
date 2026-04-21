package com.vulntrack.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;

import com.vulntrack.service.AuditLogService;
import com.vulntrack.service.ReportService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ReportController {
    private final ReportService reportService;
    private final AuditLogService auditLogService;

    @GetMapping("/report/pdf")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<byte[]> downloadReport() throws Exception {
        byte[] pdf = reportService.generateComplianceReport();
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=vulntrack-report.pdf")
            .contentType(MediaType.APPLICATION_PDF)
            .body(pdf);
    }

}
