package com.vulntrack.service;

import com.vulntrack.dto.DashboardMetrics;
import com.vulntrack.enums.IssueStatus;
import com.vulntrack.enums.Severity;
import com.vulntrack.repository.IssueRepository;
import com.vulntrack.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardService {
    private final IssueRepository repo;

    public DashboardMetrics getMetrics() {
        // Get Current User Context
        var currentUser = SecurityUtils.getCurrentUser();
        Long userId = currentUser.id();
        
        // Determine Role
        String role = currentUser.getAuthorities().stream()
            .findFirst()
            .map(a -> a.getAuthority())
            .orElse("ROLE_DEVELOPER");

        boolean isAdminOrSecAdmin = role.equals("ROLE_ADMIN") || role.equals("ROLE_SECURITY_ADMIN");

        Map<String, Long> breakdown = new HashMap<>();
        long totalIssues;
        long openIssues;
        long criticalIssues;
        double avgResolutionDays;

        if (isAdminOrSecAdmin) {
            // GLOBAL VIEW: See everything
            totalIssues = repo.count();
            openIssues = repo.countByStatus(IssueStatus.OPEN);
            criticalIssues = repo.countBySeverity(Severity.CRITICAL);
            avgResolutionDays = repo.findAverageResolutionDays() != null ? repo.findAverageResolutionDays() : 0.0;

            // Global Severity Breakdown
            for (Object[] row : repo.countGroupedBySeverity()) {
                breakdown.put(row[0].toString(), (Long) row[1]);
            }
        } else {
            // DEVELOPER VIEW: See only assigned/owned issues
            totalIssues = repo.countByOwner_IdOrAssignee_Id(userId);
            openIssues = repo.countByOwner_IdOrAssignee_IdAndStatus(userId, IssueStatus.OPEN);
            
            // For developers, "Critical" might be less relevant than "High Priority Tasks"
            // But we can still show how many of their issues are critical
            criticalIssues = repo.countByOwner_IdOrAssignee_IdAndSeverity(userId, Severity.CRITICAL);
            
            // Personal Average Resolution Time
            avgResolutionDays = repo.findAverageResolutionDaysByUser(userId) != null ? 
                                repo.findAverageResolutionDaysByUser(userId) : 0.0;

            // Personal Severity Breakdown
            for (Object[] row : repo.countGroupedBySeverityForUser(userId)) {
                breakdown.put(row[0].toString(), (Long) row[1]);
            }
        }

        return new DashboardMetrics(
            totalIssues,
            openIssues,
            criticalIssues,
            avgResolutionDays,
            breakdown
        );
    }
}