package com.vulntrack.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.vulntrack.dto.IssueResponse;
import com.vulntrack.enums.IssueStatus;
import com.vulntrack.enums.Severity;
import com.vulntrack.model.Issue;
import com.vulntrack.model.User;

@Repository
public interface IssueRepository extends JpaRepository<Issue, Long> {

    boolean existsByTitle(String title);

    List<Issue> findByCreatedBy(User user);

    List<Issue> findByAdminCreated(boolean adminCreated);

    List<Issue> findAllByOwnerId(Long ownerId);

    Page<Issue> findByDescriptionContainingIgnoreCase(String keyword, Pageable pageable);

    Page<IssueResponse> findAllByOwnerId(Long ownerId, Pageable pageable);

    long countByStatus(IssueStatus status);

    long countBySeverity(Severity severity);

    @Query("SELECT COUNT(i) FROM Issue i WHERE i.severity = 1")
    long countLowSeverity();

    @Query("SELECT COUNT(i) FROM Issue i WHERE i.severity = 4")
    long countCriticalSeverity();

    @Query("SELECT i.severity, COUNT(i) FROM Issue i GROUP BY i.severity")
    List<Object[]> countGroupedBySeverity();

    @Query("SELECT AVG(DATEDIFF(day, i.createdAt, i.updatedAt)) FROM Issue i WHERE i.status = 'RESOLVED'")
    Double findAverageResolutionDays();
}