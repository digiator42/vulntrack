package com.megastar.vulntrack.exception;

public class DuplicateIssueException extends RuntimeException {
    public DuplicateIssueException(String message) {
        super(message);
    }
}
