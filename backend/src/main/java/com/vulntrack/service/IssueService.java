package com.vulntrack.service;

import com.vulntrack.dto.IssuePatchRequest;
import com.vulntrack.dto.IssueResponse;
import com.vulntrack.dto.SearchCriteria;
import com.vulntrack.dto.UpdateIssueRequest;
import com.vulntrack.enums.Role;
import com.vulntrack.exception.DuplicateItemException;
import com.vulntrack.exception.AccessDeniedException;
import com.vulntrack.exception.BadErrorRequestException;
import com.vulntrack.exception.IssueNotFoundException;
import com.vulntrack.model.Issue;
import com.vulntrack.model.IssueSortFields;
import com.vulntrack.model.SecurityUser;
import com.vulntrack.model.User;
import com.vulntrack.repository.IssueRepository;
import com.vulntrack.repository.UserRepository;
import com.vulntrack.utils.IssueSpecification;
import com.vulntrack.utils.SecurityUtils;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.domain.Page;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class IssueService {
    private final IssueRepository issueRepository;
    private final UserRepository userRepository;

    public Issue create(Issue issue) {
        if (!SecurityUtils.isSecurityAdmin()) {
            throw new AccessDeniedException(
                "Only Security Admins can create new vulnerability records."
            );
        }

        // Enforce ownership to the creator
        Long userId = SecurityUtils.getCurrentUser().id();

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        issue.setOwner(user);

        if (issueRepository.existsByTitle(issue.getTitle())) {
            throw new DuplicateItemException("Issue with this title already exists");
        }
        return issueRepository.save(issue);
    }

    public Issue getById(Long id) {
        Issue issue = issueRepository.findById(id)
            .orElseThrow(() -> new IssueNotFoundException(id));

        SecurityUser user = SecurityUtils.getCurrentUser();

        // Admin/SecurityAdmin sees everything
        if (SecurityUtils.isSecurityAdmin()) {
            return issue;
        }

        // Developer sees only owned or assigned issues
        boolean isOwner = issue.getOwnerId().equals(user.id());
        boolean isAssignee = issue.getAssignee() != null
            && issue.getAssignee().getId().equals(user.id());

        if (isOwner || isAssignee) {
            return issue;
        }

        throw new AccessDeniedException("You do not have permission to view this issue.");
    }

    public List<Issue> getAll() { return issueRepository.findAll(); }

    public List<Issue> getAllByOwnerId(Long ownerId) {
        User owner = userRepository.findById(ownerId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        return issueRepository.findAllByOwner(owner);
    }

    public Map<String, Object> getStats() {
        List<Issue> issues = this.getAll();
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalIssues", issues.size());
        stats.put("lowIssues", issues.stream().filter(i -> i.getSeverity().equals(1)).count());
        stats.put("criticalIssues", issues.stream().filter(i -> i.getSeverity().equals(4)).count());
        return stats;
    }

    @Transactional
    public Issue updateIssue(Long id, UpdateIssueRequest request) {
        Issue issue = getById(id);
        SecurityUser user = SecurityUtils.getCurrentUser();

        // If Developer, restrict fields
        if (!SecurityUtils.isSecurityAdmin()) {
            if (request.title() != null || request.description() != null) {
                throw new AccessDeniedException(
                    "Developers can only update the status of an issue."
                );
            }
        }

        // Apply updates
        // if (request.status() != null)
        // issue.setStatus(request.status());

        // Admins can update core details
        if (SecurityUtils.isSecurityAdmin()) {
            if (request.title() != null)
                issue.setTitle(request.title());
            if (request.description() != null)
                issue.setDescription(request.description());
            // if (request.severity() != null)
            // issue.setSeverity(request.severity());
        }

        return issueRepository.save(issue);
    }

    public void deleteIssue(Long id) {
        if (!SecurityUtils.isAdmin()) {
            throw new AccessDeniedException("Unauthorized");
        }
        Issue issue = getById(id); // Ensure they can see it first
        issueRepository.delete(issue);
    }

    public IssueResponse patchIssue(
        Long id,
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

        issueRepository.save(issue);

        return IssueResponse.from(issue);
    }

    public Page<IssueResponse> getSortedListIssues(Pageable pageable) {

        if (pageable.getPageSize() > 50) {
            throw new BadErrorRequestException("Page size too large");
        }

        if (pageable.getSort().stream().count() > 1) {
            throw new BadErrorRequestException("Only one sort field allowed");
        }

        for (var order : pageable.getSort()) {
            System.out.println("---->> " + order.getProperty());
            if (!IssueSortFields.isValid(order.getProperty())) {
                throw new BadErrorRequestException("Invalid sort field: " + order.getProperty());
            }
        }

        SecurityUser user = SecurityUtils.getCurrentUser();

        if (SecurityUtils.isSecurityAdmin()) {
            return issueRepository.findAll(pageable).map(IssueResponse::from);
        } else {
            // Developers see only their assigned/owned issues
            return issueRepository.findAllByOwner_IdOrAssignee_Id(user.id(), user.id(), pageable)
                .map(IssueResponse::from);
        }

    }

    public Page<IssueResponse> searchWithSpecifications(
        List<SearchCriteria> params,
        Pageable pageable
    ) {
        Specification<Issue> spec = Specification.where(null);

        for (SearchCriteria criteria : params) {
            System.out.println(
                "Adding criteria: " + criteria.getKey() + " " + criteria.getOperation() + " "
                    + criteria.getValue()
            );
            spec = spec.and(new IssueSpecification(criteria));
        }

        return issueRepository.findAll(spec, pageable).map(IssueResponse::from);
    }

    public boolean isOwner(Long issueId, Object principal) {
        // Safety Check: Ensure principal is our custom user
        if (!(principal instanceof SecurityUser securityUser)) {
            return false;
        }

        // Role-Based Bypass: Admins and Security Admins see everything
        String role = securityUser.getAuthorities().stream()
            .findFirst()
            .map(a -> a.getAuthority())
            .orElse("");

        System.out.println("=========>>> " + role);

        if (Role.ROLE_ADMIN.name().equals(role) ||
            Role.ROLE_SECURITY_ADMIN.name().equals(role)) {
            return true;
        }

        // Ownership Check for Developers (or any non-admin)
        Issue issue = issueRepository.findById(issueId).orElse(null);
        if (issue == null)
            return false;

        Long userId = securityUser.id();

        // Check if user is the Creator (Owner)
        if (issue.getOwnerId().equals(userId)) {
            return true;
        }

        // Check if user is the Assignee (Fixer)
        if (issue.getAssignee() != null && issue.getAssignee().getId().equals(userId)) {
            return true;
        }

        return false;
    }
}
