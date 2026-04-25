package com.vulntrack.dto;

import java.time.LocalDateTime;

import com.vulntrack.enums.IssueStatus;
import com.vulntrack.enums.Severity;
import com.vulntrack.model.Issue;

import com.vulntrack.dto.UserResponse;

public record IssueResponse(
    Long id,
    String title,
    String description,
    Severity severity,
    IssueStatus status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    UserResponse assignedTo
) {
    public static IssueResponse from(Issue issue) {
        return new IssueResponse(
            issue.getId(),
            issue.getTitle(),
            issue.getDescription(),
            issue.getSeverity(),
            issue.getStatus(),
            issue.getCreatedAt(),
            issue.getUpdatedAt(),
            null
        );
    }

    public static IssueResponse withAsignee(Issue issue) {

        return new IssueResponse(
            issue.getId(),
            issue.getTitle(),
            issue.getDescription(),
            issue.getSeverity(),
            issue.getStatus(),
            issue.getCreatedAt(),
            issue.getUpdatedAt(),
            UserResponse.from(issue.getAssignee())
        );
    }
}
