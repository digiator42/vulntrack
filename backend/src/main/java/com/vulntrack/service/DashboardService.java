package com.vulntrack.service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.vulntrack.dto.DashboardMetrics;
import com.vulntrack.enums.IssueStatus;
import com.vulntrack.enums.Severity;
import com.vulntrack.repository.IssueRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DashboardService {
    private final IssueRepository repo;

    public DashboardMetrics getMetrics() {
        Map<String, Long> breakdown = new HashMap<>();
        for (Object[] row : repo.countGroupedBySeverity()) {
            breakdown.put(row[0].toString(), (Long) row[1]);
        }

        return new DashboardMetrics(
            repo.count(),
            repo.countByStatus(IssueStatus.OPEN),
            repo.countBySeverity(Severity.CRITICAL),
            repo.findAverageResolutionDays() != null ? repo.findAverageResolutionDays() : 0.0,
            breakdown
        );
    }
}