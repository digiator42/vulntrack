package com.vulntrack.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import org.springframework.stereotype.Service;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.vulntrack.model.Issue;
import com.vulntrack.repository.IssueRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {
    private final IssueRepository issueRepo;

    public byte[] generateComplianceReport() throws DocumentException, IOException {
        Document document = new Document();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);
        document.open();

        // Title
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
        Paragraph title = new Paragraph("VulnTrack Compliance Report", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);
        document.add(new Paragraph("\n"));

        // Table
        PdfPTable table = new PdfPTable(5);
        table.addCell("ID");
        table.addCell("Title");
        table.addCell("Severity");
        table.addCell("Status");
        table.addCell("CVSS");

        List<Issue> issues = issueRepo.findAll();
        for (Issue issue : issues) {
            table.addCell(String.valueOf(issue.getId()));
            table.addCell(issue.getTitle());
            table.addCell(issue.getSeverity().toString());
            table.addCell(issue.getStatus().toString());
            table.addCell(issue.getCvssScore() != null ? issue.getCvssScore().toString() : "N/A");
        }
        document.add(table);
        document.close();

        return out.toByteArray();
    }
}