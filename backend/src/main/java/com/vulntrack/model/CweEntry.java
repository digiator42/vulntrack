package com.vulntrack.model;

import lombok.Data;

@Data
public class CweEntry {
    private String id;

    private String name;

    private String description;

    private String remediation;
}