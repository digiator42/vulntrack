package com.vulntrack.dto;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

import com.vulntrack.enums.Severity;
import com.vulntrack.model.User;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateIssueRequest(
    @NotBlank(message = "Title is required") String title,

    @NotNull(message = "Severity is required") Severity severity,

    String description,

    String cvssVector,

    String cweId,

    Double cvssScore,

    @AuthenticationPrincipal User user
) {
}
