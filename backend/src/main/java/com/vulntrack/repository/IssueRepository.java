package com.vulntrack.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.vulntrack.dto.IssueResponse;
import com.vulntrack.enums.IssueStatus;
import com.vulntrack.enums.Severity;
import com.vulntrack.model.Issue;
import com.vulntrack.model.User;

@Repository
public interface IssueRepository extends JpaRepository<Issue, Long> {

    boolean existsByTitle(String title);

    Page<Issue> findAll(Specification<Issue> specification, Pageable pageable);

    List<Issue> findAllByOwner(User owner);

    Page<Issue> findAllByOwner_IdOrAssignee_Id(Long ownerId, Long assigneeId, Pageable pageable);

    List<Issue> findAllByOwner_IdOrAssignee_Id(Long ownerId, Long assigneeId);

    Page<Issue> findByDescriptionContainingIgnoreCase(String keyword, Pageable pageable);

    Page<IssueResponse> findAllByOwnerId(Long ownerId, Pageable pageable);

    long countByStatus(IssueStatus status);

    long countBySeverity(Severity severity);

   @Query("SELECT i.severity, COUNT(i) FROM Issue i GROUP BY i.severity")
    List<Object[]> countGroupedBySeverity();
    
    @Query("SELECT AVG(DATEDIFF(day, i.createdAt, i.updatedAt)) FROM Issue i WHERE i.status = 'RESOLVED'")
    Double findAverageResolutionDays();

    // Count issues where user is Owner OR Assignee
    @Query("SELECT COUNT(i) FROM Issue i WHERE i.owner.id = :uid OR i.assignee.id = :uid")
    long countByOwner_IdOrAssignee_Id(@Param("uid") Long userId);

    // Count open issues for user
    @Query("SELECT COUNT(i) FROM Issue i WHERE (i.owner.id = :uid OR i.assignee.id = :uid) AND i.status = :status")
    long countByOwner_IdOrAssignee_IdAndStatus(
        @Param("uid") Long userId, 
        @Param("status") IssueStatus status
    );

    // Count critical issues for user
    @Query("SELECT COUNT(i) FROM Issue i WHERE (i.owner.id = :uid OR i.assignee.id = :uid) AND i.severity = :sev")
    long countByOwner_IdOrAssignee_IdAndSeverity(
        @Param("uid") Long userId, 
        @Param("sev") Severity severity
    );

    // Personal Avg Resolution Time
    @Query("SELECT AVG(DATEDIFF(day, i.createdAt, i.updatedAt)) FROM Issue i WHERE (i.owner.id = :uid OR i.assignee.id = :uid) AND i.status = 'RESOLVED'")
    Double findAverageResolutionDaysByUser(@Param("uid") Long userId);

    // Personal Severity Breakdown
    @Query("SELECT i.severity, COUNT(i) FROM Issue i WHERE i.owner.id = :uid OR i.assignee.id = :uid GROUP BY i.severity")
    List<Object[]> countGroupedBySeverityForUser(@Param("uid") Long userId);

    // List Issues for Developer (for the main list view)
    @Query("SELECT i FROM Issue i WHERE i.owner.id = :uid OR i.assignee.id = :uid")
    Page<Issue> findAllByOwnerIdOrAssigneeId(@Param("uid") Long userId, Pageable pageable);

}