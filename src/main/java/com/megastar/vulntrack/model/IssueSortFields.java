package com.megastar.vulntrack.model;

import java.util.Arrays;

public enum IssueSortFields {
    CREATED_AT("createdAt"),
    SEVERITY("severity"),
    TITLE("title"),
    STATUS("status");

    private final String field;

    private IssueSortFields(String field) {
        this.field = field;
    }

    public static boolean isValid(String value) {
        return Arrays.stream(values()).anyMatch(v -> v.field.equals(value));
    }
}
