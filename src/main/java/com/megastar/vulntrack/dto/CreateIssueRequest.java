package com.megastar.vulntrack.dto;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

import com.megastar.vulntrack.model.User;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateIssueRequest(
        @NotBlank(message = "Title is required") String title,
        String description,
        @NotNull(message = "Severity is required") Integer severity,
        @AuthenticationPrincipal User user
) {}
