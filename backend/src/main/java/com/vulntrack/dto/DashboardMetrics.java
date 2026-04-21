package com.vulntrack.dto;

import java.util.Map;

public record DashboardMetrics(
    long totalIssues,
    long openIssues,
    long criticalIssues,
    double avgResolutionDays,
    Map<String, Long> severityBreakdown
) {
}