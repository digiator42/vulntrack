package com.vulntrack.controller;

import com.vulntrack.config.UserContext;
import com.vulntrack.dto.CreateIssueRequest;
import com.vulntrack.dto.IssuePageRequest;
import com.vulntrack.dto.IssuePatchRequest;
import com.vulntrack.dto.IssueResponse;
import com.vulntrack.dto.UpdateIssueRequest;
import com.vulntrack.exception.AccessDeniedException;
import com.vulntrack.model.Issue;
import com.vulntrack.service.IssueService;
import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/issues")
public class IssueController {
    private final IssueService service;
    private final UserContext userContext;

    public IssueController(IssueService issueService, UserContext userContext) {
        this.service = issueService;
        this.userContext = userContext;
    }

    @GetMapping("/me")
    public String getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        return "Logged in as: " + userDetails.getUsername();
    }

    @PostMapping
    public Issue createIssue(@Valid @RequestBody CreateIssueRequest issueRequest) {
        Long userId = userContext.getUserId();

        Issue issue = new Issue(
            null,
            issueRequest.title(),
            issueRequest.description(),
            issueRequest.severity(),
            userId
        );
        return this.service.create(issue, userId);
    }

    @GetMapping("/{id}")
    public Issue findById(@PathVariable long id) {
        Issue issue = service.getById(id);

        if (!issue.getOwnerId().equals(userContext.getUserId())) {
            throw new AccessDeniedException("Access denied");
        }
        return issue;
    }

    @PutMapping("/{id}")
    public Issue updateIssue(
        @PathVariable Long id,
        @Valid @RequestBody UpdateIssueRequest request
    ) {
        return this.service.updateIssue(id, userContext.getUserId(), request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteIssue(@PathVariable Long id) {
        this.service.deleteIssue(id, userContext.getUserId());
    }

    @PatchMapping("/{id}")
    public IssueResponse patchIssue(
        @PathVariable Long id,
        @Valid @RequestBody IssuePatchRequest request
    ) throws Exception {
        return this.service.patchIssue(id, userContext.getUserId(), request);
    }

    @GetMapping()
    public Page<IssueResponse> listIssues(@Valid IssuePageRequest request) throws Exception {
        return this.service.getSortedListIssues(userContext.getUserId(), request.toPeagable());
    }

}
