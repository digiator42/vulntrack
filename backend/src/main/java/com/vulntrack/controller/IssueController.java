package com.vulntrack.controller;

import com.vulntrack.annotation.AuditLog;
import com.vulntrack.dto.CreateIssueRequest;
import com.vulntrack.dto.IssuePageRequest;
import com.vulntrack.dto.IssuePatchRequest;
import com.vulntrack.dto.IssueResponse;
import com.vulntrack.dto.SearchCriteria;
import com.vulntrack.dto.UpdateIssueRequest;
import com.vulntrack.model.Issue;
import com.vulntrack.service.IssueService;
import com.vulntrack.utils.SecurityUtils;

import jakarta.validation.Valid;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/issues")
public class IssueController {
    private final IssueService service;

    public IssueController(IssueService issueService) { this.service = issueService; }

    @GetMapping("/me")
    public String getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        return "Logged in as: " + userDetails.getUsername();
    }

    @PostMapping
    public Issue createIssue(
        @Valid @RequestBody CreateIssueRequest issueRequest
    ) {

        Long userId = SecurityUtils.getCurrentUser().id();

        Issue issue = new Issue(
            issueRequest.title(),
            issueRequest.description(),
            issueRequest.severity(),
            userId
        );
        return this.service.create(issue, userId);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@issueService.isOwner(#id, authentication.principal.id)")
    @AuditLog(action = "ACCESS_ISSUE", resource = "ISSUE", id = "#id")
    public Issue findById(@PathVariable long id) {
        Issue issue = service.getById(id);

        return issue;
    }

    @PutMapping("/{id}")
    public Issue updateIssue(
        @PathVariable Long id,
        @Valid @RequestBody UpdateIssueRequest request
    ) {

        Long userId = SecurityUtils.getCurrentUser().id();
        return this.service.updateIssue(id, userId, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteIssue(
        @PathVariable Long id
    ) {
        this.service.deleteIssue(id, SecurityUtils.getCurrentUser().id());
    }

    @PatchMapping("/{id}")
    public IssueResponse patchIssue(
        @PathVariable Long id,
        @Valid @RequestBody IssuePatchRequest request
    ) throws Exception {
        return this.service.patchIssue(id, SecurityUtils.getCurrentUser().id(), request);
    }

    @GetMapping()
    public Page<IssueResponse> listIssues(
        @Valid IssuePageRequest request
    ) throws Exception {
        return this.service
            .getSortedListIssues(SecurityUtils.getCurrentUser().id(), request.toPeagable());
    }

    @PostMapping("/search")
    public Page<IssueResponse> search(@RequestBody List<SearchCriteria> params, Pageable pageable) {
        return service.searchWithSpecifications(params, pageable);
    }

}
