package com.vulntrack.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateIssueRequest(
    @NotBlank(message = "Title is required") String title,
    String description,
    @NotNull(message = "Severity is required") Integer severity
) {
}
