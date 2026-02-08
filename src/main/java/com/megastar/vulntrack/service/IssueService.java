package com.megastar.vulntrack.service;

import com.megastar.vulntrack.dto.IssuePatchRequest;
import com.megastar.vulntrack.dto.IssueResponse;
import com.megastar.vulntrack.dto.UpdateIssueRequest;
import com.megastar.vulntrack.exception.AccessDeniedException;
import com.megastar.vulntrack.exception.DuplicateIssueException;
import com.megastar.vulntrack.exception.BadErrorRequestException;
import com.megastar.vulntrack.exception.IssueNotFoundException;
import com.megastar.vulntrack.model.Issue;
import com.megastar.vulntrack.model.IssueSortFields;
import com.megastar.vulntrack.repository.IssueRepository;

import org.springframework.stereotype.Service;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class IssueService {
    private final IssueRepository issueRepository;

    public IssueService(IssueRepository issueRepository) {
        this.issueRepository = issueRepository;
    }

    public Issue create(Issue issue, Long userId) {
        boolean exists = issueRepository.existsByTitle(issue.getTitle());
        if (exists) {
            throw new DuplicateIssueException(
                    "Issue with this title already exists");
        }
        return issueRepository.save(issue);
    }

    public List<Issue> getAll() {
        return issueRepository.findAll();
    }

    public Issue getById(long id) {
        return issueRepository.findById(id).orElseThrow(() -> new IssueNotFoundException(id));
    }

    public List<Issue> getAllByOwnerId(Long ownerId) {
        return issueRepository.findAllByOwnerId(ownerId);
    }

    public Map<String, Object> getStats() {
        List<Issue> issues = this.getAll();
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalIssues", issues.size());
        stats.put("lowIssues", issues.stream().filter(i -> i.getSeverity().equals(1)).count());
        stats.put("criticalIssues", issues.stream().filter(i -> i.getSeverity().equals(4)).count());
        return stats;
    }

    public Issue updateIssue(Long id, Long userId, UpdateIssueRequest request) {
        Issue currentIssue = this.getById(id);

        if (!currentIssue.getOwnerId().equals(userId)) {
            throw new AccessDeniedException(request.title());
        }

        currentIssue.setId(id);
        currentIssue.setTitle(request.title());
        currentIssue.setDescription(request.description());

        return currentIssue;
    }

    public void deleteIssue(Long id, Long userId) {

        Issue currentIssue = this.getById(id);

        if (!currentIssue.getOwnerId().equals(userId)) {
            throw new AccessDeniedException("You do not own this issue");
        }

        issueRepository.delete(currentIssue);
    }

    public IssueResponse patchIssue(
            Long id,
            Long userId,
            IssuePatchRequest req) throws Exception {

        Issue issue = this.getById(id);

        if (!issue.getOwnerId().equals(userId)) {
            throw new AccessDeniedException(issue.getTitle());
        }

        if (req.title() != null) {
            issue.setTitle(req.title());
        }

        if (req.description() != null) {
            issue.setDescription(req.description());
        }

        if (req.status() != null) {
            issue.setStatus(req.status());
        }

        if (!issue.isUpdated()) {
            throw new BadErrorRequestException("No fields provided for update");
        }

        return IssueResponse.from(issue);
    }

    public Page<IssueResponse> getSortedListIssues(Long ownerId, Pageable pageable) {

        if (pageable.getPageSize() > 50) {
            throw new BadErrorRequestException("Page size too large");
        }

        System.out.println("sort count ===>> " + pageable.getSort().stream().count());

        if (pageable.getSort().stream().count() > 1) {
            throw new BadErrorRequestException("Only one sort field allowed");
        }

        for (var order : pageable.getSort()) {
            System.out.println("---->> " + order.getProperty());
            if (!IssueSortFields.isValid(order.getProperty())) {
                throw new BadErrorRequestException("Invalid sort field: " + order.getProperty());
            }
        }

        return issueRepository.findAllByOwnerId(ownerId, pageable);
    }
}
