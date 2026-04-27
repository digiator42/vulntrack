package com.vulntrack.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.vulntrack.model.Comment;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByIssueIdOrderByCreatedAtAsc(Long issueId);
}