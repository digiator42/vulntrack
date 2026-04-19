package com.megastar.vulntrack.dto;

import com.megastar.vulntrack.model.Issue;
import com.megastar.vulntrack.model.IssueStatus;

public record IssueResponse(
        Long id,
        String title,
        String description,
        Integer severity,
        IssueStatus status) {
    public static IssueResponse from(Issue issue) {
        return new IssueResponse(
                issue.getId(),
                issue.getTitle(),
                issue.getDescription(),
                issue.getSeverity(),
                issue.getStatus());
    }
}
