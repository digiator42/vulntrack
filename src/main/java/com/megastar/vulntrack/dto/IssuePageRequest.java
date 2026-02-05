package com.megastar.vulntrack.dto;

import com.megastar.vulntrack.model.IssueSortFields;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record IssuePageRequest(
        @Min(0) Integer page,
        @Min(1) @Max(50) Integer size,
        String sort
    ) {
    public IssuePageRequest {
        page = (page == null) ? 0 : page;
        size = (size == null) ? 10 : size;
        sort = (sort == null) ? String.valueOf(IssueSortFields.CREATED_AT) : sort;
    }
}
