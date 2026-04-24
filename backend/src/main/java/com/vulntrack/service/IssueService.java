package com.vulntrack.service;

import com.vulntrack.dto.IssuePatchRequest;
import com.vulntrack.dto.IssueResponse;
import com.vulntrack.dto.SearchCriteria;
import com.vulntrack.dto.UpdateIssueRequest;
import com.vulntrack.exception.DuplicateItemException;
import com.vulntrack.exception.BadErrorRequestException;
import com.vulntrack.exception.IssueNotFoundException;
import com.vulntrack.model.Issue;
import com.vulntrack.model.IssueSortFields;
import com.vulntrack.model.User;
import com.vulntrack.repository.IssueRepository;
import com.vulntrack.utils.IssueSpecification;

import org.springframework.stereotype.Service;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.domain.Page;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class IssueService {
    private final IssueRepository issueRepository;

    public IssueService(IssueRepository issueRepository) { this.issueRepository = issueRepository; }

    public Issue create(Issue issue, User owner) {
        boolean exists = issueRepository.existsByTitle(issue.getTitle());
        if (exists) {
            throw new DuplicateItemException(
                "Issue with this title already exists"
            );
        }
        issue.setOwner(owner);
        return issueRepository.save(issue);
    }

    public List<Issue> getAll() { return issueRepository.findAll(); }

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

    public Issue updateIssue(Long id, User owner, UpdateIssueRequest request) {
        Issue currentIssue = this.getById(id);

        currentIssue.setId(id);
        currentIssue.setTitle(request.title());
        currentIssue.setDescription(request.description());

        return issueRepository.save(currentIssue);
    }

    public void deleteIssue(Long id, User owner) {

        Issue currentIssue = this.getById(id);

        issueRepository.delete(currentIssue);
    }

    public IssueResponse patchIssue(
        Long id,
        User owner,
        IssuePatchRequest req
    ) throws Exception {

        Issue issue = this.getById(id);

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
        issueRepository.save(issue);

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

    public Page<IssueResponse> searchWithSpecifications(
        List<SearchCriteria> params,
        Pageable pageable
    ) {
        Specification<Issue> spec = Specification.where(null);

        for (SearchCriteria criteria : params) {
            System.out.println("Adding criteria: " + criteria.getKey() + " " + criteria.getOperation() + " " + criteria.getValue());
            spec = spec.and(new IssueSpecification(criteria));
        }

        return issueRepository.findAll(spec, pageable).map(IssueResponse::from);
    }

    public boolean isOwner(Long issueId, User owner) {
        return issueRepository.findById(issueId)
            .map(Issue::getOwner)
            .map(User::getId)
            .map(ownerId -> ownerId.equals(owner.getId()))
            .orElse(false);
    }
}
