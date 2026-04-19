package com.vulntrack.dto;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

import com.vulntrack.model.User;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateIssueRequest(
                @NotBlank(message = "Title is required") String title,
                String description,
                @NotNull(message = "Severity is required") @Min(1) @Max(4) Integer severity,
                @AuthenticationPrincipal User user) {
}
