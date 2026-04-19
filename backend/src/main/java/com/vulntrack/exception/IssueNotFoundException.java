package com.vulntrack.exception;

public class IssueNotFoundException extends RuntimeException {

    public IssueNotFoundException(Long id) {
        super("Issue id: {" + id + "} not found");
    }
}
