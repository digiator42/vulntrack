package com.vulntrack.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.vulntrack.model.Issue;
import com.vulntrack.enums.Role;
import com.vulntrack.repository.IssueRepository;
import com.vulntrack.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {
    private final IssueRepository issueRepository;

    private static final Color PRIMARY_COLOR = new Color(70, 130, 180); // Steel Blue
    private static final Color SECONDARY_COLOR = new Color(245, 245, 245); // Light Gray
    private static final Color ACCENT_COLOR = new Color(255, 99, 71); // Tomato Red

    public byte[] generateRoleBasedReport() throws Exception {
        var currentUser = SecurityUtils.getCurrentUser();
        String role = currentUser.getAuthorities().stream()
            .findFirst()
            .map(a -> a.getAuthority())
            .orElse("ROLE_DEVELOPER");

        List<Issue> issues;
        String reportTitle;

        // Role-Based Data Filtering
        if (role.equals(Role.ROLE_ADMIN.name()) || role.equals(Role.ROLE_SECURITY_ADMIN.name())) {
            issues = issueRepository.findAll();
            reportTitle = "Global Vulnerability Compliance Report";
        } else {
            // Developers only see their assigned/owned issues
            issues = issueRepository
                .findAllByOwner_IdOrAssignee_Id(currentUser.id(), currentUser.id());
            reportTitle = "Personal Remediation Report for " + currentUser.username();
        }

        return generatePdf(reportTitle, issues, currentUser.username());
    }

    private byte[] generatePdf(String title, List<Issue> issues, String generatedBy)
        throws Exception {
        Document document = new Document(PageSize.A4);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);
        document.open();

        // --- Header ---
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, PRIMARY_COLOR);
        Paragraph titlePara = new Paragraph(title, titleFont);
        titlePara.setAlignment(Element.ALIGN_CENTER);
        document.add(titlePara);

        Font metaFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.GRAY);
        Paragraph meta = new Paragraph(
            "Generated on: "
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                + " by " + generatedBy,
            metaFont
        );
        meta.setAlignment(Element.ALIGN_CENTER);
        meta.setSpacingAfter(20);
        document.add(meta);

        // --- Summary Stats ---
        long critical = issues.stream().filter(i -> i.getSeverity().name().equals("CRITICAL"))
            .count();
        long high = issues.stream().filter(i -> i.getSeverity().name().equals("HIGH")).count();

        Font statFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
        document.add(
            new Paragraph(
                "Summary: " + issues.size() + " Total Issues (" + critical + " Critical, " + high
                    + " High)",
                statFont
            )
        );
        document.add(new Paragraph("\n"));

        // --- Table ---
        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10f);
        table.setSpacingAfter(10f);

        // Headers
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
        String[] headers = {
                "ID", "Title", "Severity", "Status", "CVSS"
        };
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, headerFont));
            cell.setBackgroundColor(Color.DARK_GRAY);
            table.addCell(cell);
        }

        // Rows
        Font rowFont = FontFactory.getFont(FontFactory.HELVETICA, 9);
        for (Issue issue : issues) {
            table.addCell(new Phrase(String.valueOf(issue.getId()), rowFont));
            table.addCell(new Phrase(issue.getTitle(), rowFont));
            table.addCell(new Phrase(issue.getSeverity().toString(), rowFont));
            table.addCell(new Phrase(issue.getStatus().toString(), rowFont));
            table.addCell(
                new Phrase(
                    issue.getCvssScore() != null ? issue.getCvssScore().toString() : "N/A", rowFont
                )
            );
        }

        document.add(table);
        document.close();
        return out.toByteArray();
    }

    public Map<String, Object> getJsonSummary() {
        var currentUser = SecurityUtils.getCurrentUser();
        String role = currentUser.getAuthorities().stream()
            .findFirst()
            .map(a -> a.getAuthority())
            .orElse("ROLE_DEVELOPER");

        List<Issue> issues;

        // Role-Based Data Filtering
        if (role.equals(Role.ROLE_ADMIN.name()) || role.equals(Role.ROLE_SECURITY_ADMIN.name())) {
            issues = issueRepository.findAll();
        } else {
            // Developers only see their assigned/owned issues
            issues = issueRepository
                .findAllByOwner_IdOrAssignee_Id(currentUser.id(), currentUser.id());
        }

        return buildSummaryMap(issues);
    }

    private Map<String, Object> buildSummaryMap(List<Issue> issues) {
        Map<String, Object> summary = new HashMap<>();

        // Total Counts
        summary.put("totalIssues", issues.size());
        summary.put(
            "openIssues", issues.stream().filter(i -> i.getStatus().name().equals("OPEN")).count()
        );
        summary.put(
            "resolvedIssues",
            issues.stream().filter(i -> i.getStatus().name().equals("RESOLVED")).count()
        );

        // Severity Breakdown (for Pie Charts)
        Map<String, Long> severityCounts = issues.stream()
            .collect(Collectors.groupingBy(i -> i.getSeverity().name(), Collectors.counting()));
        summary.put("severityBreakdown", severityCounts);

        // Average CVSS Score
        double avgCvss = issues.stream()
            .filter(i -> i.getCvssScore() != null)
            .mapToDouble(Issue::getCvssScore)
            .average()
            .orElse(0.0);
        summary.put("averageCvss", Math.round(avgCvss * 10.0) / 10.0);

        // Top CWEs (for Bar Charts)
        Map<String, Long> cweCounts = issues.stream()
            .filter(i -> i.getCweId() != null)
            .collect(Collectors.groupingBy(Issue::getCweId, Collectors.counting()));
        summary.put("topCwes", cweCounts);

        return summary;
    }
}