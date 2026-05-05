package com.vulntrack.dto;

public record CveResult(
    String id,
    String description,
    Double cvssScore,
    String cvssVersion,
    String severity,
    String vectorString,
    String publishedDate,
    String cweId
) {
}