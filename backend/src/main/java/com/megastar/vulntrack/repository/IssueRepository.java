package com.megastar.vulntrack.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.megastar.vulntrack.dto.IssueResponse;
import com.megastar.vulntrack.model.Issue;
import com.megastar.vulntrack.model.User;

@Repository
public interface IssueRepository extends JpaRepository<Issue, Long> {

    boolean existsByTitle(String title);

    List<Issue> findByCreatedBy(User user);

    List<Issue> findByAdminCreated(boolean adminCreated);

    List<Issue> findAllByOwnerId(Long ownerId);

    Page<Issue> findByDescriptionContainingIgnoreCase(String keyword, Pageable pageable);

    Page<IssueResponse> findAllByOwnerId(Long ownerId, Pageable pageable);
}