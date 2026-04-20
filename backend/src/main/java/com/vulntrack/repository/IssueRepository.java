package com.vulntrack.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.vulntrack.dto.IssueResponse;
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

    @Query("SELECT COUNT(i) FROM Issue i WHERE i.severity = 1")
    long countLowSeverity();

    @Query("SELECT COUNT(i) FROM Issue i WHERE i.severity = 4")
    long countCriticalSeverity();
}