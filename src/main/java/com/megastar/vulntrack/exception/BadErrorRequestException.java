package com.megastar.vulntrack.exception;

public class BadErrorRequestException extends RuntimeException {
    public BadErrorRequestException(String message) {
        super(message);
    }
}