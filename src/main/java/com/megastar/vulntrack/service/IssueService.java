package com.megastar.vulntrack.service;

import com.megastar.vulntrack.config.UserContext;
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

import java.util.List;

@Service
public class IssueService {
    private final IssueRepository issueRepository = new IssueRepository();

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
            throw new AccessDeniedException(currentIssue.getTitle());
        }

        issueRepository.delete(id);
    }

    public IssueResponse patchIssue(
            Long id,
            Long userId,
            IssuePatchRequest req) throws Exception {

        Issue issue = this.getById(id);

        if (!issue.getOwnerId().equals(userId)) {
            throw new AccessDeniedException(issue.getTitle());
        }

        boolean updated = false;

        if (req.title() != null) {
            issue.setTitle(req.title());
            updated = true;
        }

        if (req.description() != null) {
            issue.setDescription(req.description());
            updated = true;
        }

        if (req.status() != null) {
            issue.setStatus(req.status());
            updated = true;
        }

        if (!updated) {
            throw new BadRequestException("No fields provided for update");
        }

        return IssueResponse.from(issue);
    }

    public List<Issue> getSortedListIssues(int page, int size, String sort, Long ownerId) throws Exception {
        // /issues?page=1&size=20&sort=severity,asc

        if (size > 50) {
            throw new BadRequestException("Size is invalid");
        }

        String[] sortFields = sort.split(",");
        String field = sortFields[0];
        String direction = sortFields.length > 1 ? sortFields[1] : "asc";
        
        if (!IssueSortFields.isValid(field)) {
            throw new BadRequestException("Sort is invalid");
        }

        return issueRepository.listIssuesByOwnerId (
            page,
            size,
            field,
            direction,
            ownerId
        );

    }
}
