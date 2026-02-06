package com.megastar.vulntrack.service;

import com.megastar.vulntrack.dto.IssuePatchRequest;
import com.megastar.vulntrack.dto.IssueResponse;
import com.megastar.vulntrack.dto.UpdateIssueRequest;
import com.megastar.vulntrack.exception.AccessDeniedException;
import com.megastar.vulntrack.exception.DuplicateIssueException;
import com.megastar.vulntrack.exception.IssueNotFoundException;
import com.megastar.vulntrack.model.Issue;
import com.megastar.vulntrack.model.IssueSortFields;
import com.megastar.vulntrack.repository.IssueRepository;

import org.apache.coyote.BadRequestException;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import java.util.List;

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
            throw new BadRequestException("No fields provided for update");
        }

        return IssueResponse.from(issue);
    }

    // public List<Issue> getSortedListIssues(IssuePageRequest req, Long ownerId) {

    // String[] sortParts = req.sort().split(",");
    // String field = sortParts[0];

    // if (!IssueSortFields.isValid(field)) {
    // throw new BadRequestException("Sort field is invalid: " + field);
    // }

    // Sort.Direction direction = (sortParts.length > 1 &&
    // sortParts[1].equalsIgnoreCase("desc"))
    // ? Sort.Direction.DESC
    // : Sort.Direction.ASC;

    // // Note: 'field' must match the variable name in your Issue.java entity
    // Pageable pageable = PageRequest.of(req.page(), req.size(), Sort.by(direction,
    // field));

    // // 5. Call Repository
    // return issueRepository.findAllByOwnerId(ownerId, pageable).getContent();
    // }

    public Page<Issue> getSortedListIssues(Long ownerId, Pageable pageable) {
        // Validate sort fields against your Enum
        pageable.getSort().forEach(order -> {
            if (!IssueSortFields.isValid(order.getProperty())) {
                return;
            }
        });

        return issueRepository.findAllByOwnerId(ownerId, pageable);
    }
}
