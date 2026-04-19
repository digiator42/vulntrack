package com.vulntrack.dto;

public record ValidationError(
    String field,
    String defaultMessage
) {
}
