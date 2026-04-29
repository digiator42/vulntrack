package com.vulntrack.dto;

import lombok.Builder;
import java.util.List;

@Builder
public record DependencyScanResult(
    String manifestFileName,
    int totalDependencies,
    int vulnerableDependencies,
    List<VulnerableDependency> vulnerabilities
) {

    @Builder
    public static record VulnerableDependency(
        ParsedDependency dependency,
        List<CveVulnerability> cves,
        List<Long> createdIssueIds
    ) {
    }
}