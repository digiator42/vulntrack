package com.vulntrack.dto;

import com.vulntrack.model.IssueStatus;

import jakarta.validation.constraints.Size;

public record IssuePatchRequest(
        @Size(min = 3, max = 100) String title,
        @Size(max = 500) String description,
        IssueStatus status) {
}
