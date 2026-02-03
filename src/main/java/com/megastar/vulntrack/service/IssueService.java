package com.megastar.vulntrack.service;

import com.megastar.vulntrack.dto.UpdateIssueRequest;
import com.megastar.vulntrack.exception.AccessDeniedException;
import com.megastar.vulntrack.exception.DuplicateIssueException;
import com.megastar.vulntrack.exception.IssueNotFoundException;
import com.megastar.vulntrack.model.Issue;
import com.megastar.vulntrack.repository.IssueRepository;

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
        return issueRepository.findById(id).orElse(null);
    }

    public List<Issue> getAllByOwnerId(Long ownerId) {
        return issueRepository.findAllByOwnerId(ownerId);
    }

    public Issue updateIssue(Long id, Long userId, UpdateIssueRequest request) {
        Issue currentIssue = this.getById(id);
        
        if (currentIssue == null) {
            throw new IssueNotFoundException(id);
        }

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

        if (currentIssue == null) {
            throw new IssueNotFoundException(id);
        }
        
        if (!currentIssue.getOwnerId().equals(userId)) {
            throw new AccessDeniedException(currentIssue.getTitle());
        }

        issueRepository.delete(id);
    }
}
