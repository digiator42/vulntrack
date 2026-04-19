package com.vulntrack.model;

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

    public String getField() {
        return field;
    }

    public static boolean isValid(String value) {
        return Arrays.stream(values()).anyMatch(v -> {
            System.out.println(value);
            return v.field.equalsIgnoreCase(value);
        });
    }
}
