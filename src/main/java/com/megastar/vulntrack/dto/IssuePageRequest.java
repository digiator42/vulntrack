package com.megastar.vulntrack.dto;

import com.megastar.vulntrack.model.IssueSortFields;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.data.domain.Sort;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

public record IssuePageRequest(
        @Min(0) Integer page,
        @Min(1) @Max(50) Integer size,
        String sort) {
    public IssuePageRequest {
        page = (page == null) ? 0 : page;
        size = (size == null) ? 10 : size;
        sort = (sort == null) ? IssueSortFields.CREATED_AT.getField() : sort;
    }

    public Pageable toPeagable() {
        String field = sort.contains(",") ? sort.split(",")[0] : sort;

        System.out.println("sort in page request ===>>>> " + sort);

        Sort.Direction direction = sort.toLowerCase().contains("asc")
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        
        return PageRequest.of(page, size, Sort.by(direction, field));
    }
}
