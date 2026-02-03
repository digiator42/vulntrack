package com.megastar.vulntrack.controller;

import com.megastar.vulntrack.dto.CreateIssueRequest;
import com.megastar.vulntrack.dto.UpdateIssueRequest;
import com.megastar.vulntrack.exception.AccessDeniedException;
import com.megastar.vulntrack.exception.IssueNotFoundException;
import com.megastar.vulntrack.model.Issue;
import com.megastar.vulntrack.service.IssueService;
import jakarta.validation.Valid;
import scala.annotation.unused;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequestMapping("/issues")
public class IssueController {
    private final IssueService service;

    public IssueController(IssueService issueService) {
        this.service = issueService;
    }

    @PostMapping
    public Issue createIssue(
            @RequestHeader("X-USER-ID") Long userId,
            @Valid @RequestBody CreateIssueRequest issueRequest) {

        Issue issue = new Issue(
                null,
                issueRequest.title(),
                issueRequest.description(),
                issueRequest.severity(),
                userId);
        return this.service.create(issue, userId);
    }

    @GetMapping()
    public List<Issue> getAllIssues(@RequestHeader("X-USER-ID") Long userId) {
        return this.service.getAllByOwnerId(userId);
    }

    @GetMapping("/{id}")
    public Issue findById(@PathVariable long id, @RequestHeader("X-USER-ID") Long userId) {
        Issue issue = service.getById(id);

        if (issue == null) {
            throw new IssueNotFoundException(id);
        }

        if (!issue.getOwnerId().equals(userId)) {
            throw new AccessDeniedException("Access denied");
        }
        return issue;
    }

    @PutMapping("/{id}")
    public Issue updateIssue(
            @PathVariable Long id,
            @RequestHeader("X-USER-ID") Long userId,
            @Valid @RequestBody UpdateIssueRequest request) {
        return this.service.updateIssue(id, userId, request);
    }

    @PatchMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteIssue(
            @PathVariable Long id,
            @RequestHeader("X-USER-ID") Long userId) {
        this.service.deleteIssue(id, userId);
    }

}
