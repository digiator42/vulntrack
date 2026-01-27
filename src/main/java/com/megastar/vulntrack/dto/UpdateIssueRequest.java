package com.megastar.vulntrack.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateIssueRequest(
        @NotBlank String title,
        String description,
        @NotNull Integer severity
    ) {
}
