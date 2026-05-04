package com.vulntrack.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NvdCpeResponse(
    List<Product> products
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Product(
        Cpe cpe
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Cpe(
        String cpeName,
        String cpeNameId,
        boolean deprecated,
        List<DeprecatedBy> deprecatedBy
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DeprecatedBy(
        String cpeName,
        String cpeNameId
    ) {
    }
}