package com.vulntrack.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.vulntrack.model.ActivityLog;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {
    List<ActivityLog> findByIssueIdOrderByCreatedAtDesc(Long issueId);
}