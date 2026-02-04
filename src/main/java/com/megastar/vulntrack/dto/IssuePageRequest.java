package com.megastar.vulntrack.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record IssuePageRequest(
    @Min(0) int page,
    @Min(1) @Max(50) int size,
    String sort
) {}
