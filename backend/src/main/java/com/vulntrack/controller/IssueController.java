package com.vulntrack.controller;

import com.vulntrack.annotation.AuditLog;
import com.vulntrack.dto.CreateIssueRequest;
import com.vulntrack.dto.IssuePageRequest;
import com.vulntrack.dto.IssuePatchRequest;
import com.vulntrack.dto.IssueResponse;
import com.vulntrack.dto.SearchCriteria;
import com.vulntrack.dto.UpdateIssueRequest;
import com.vulntrack.exception.BadErrorRequestException;
import com.vulntrack.model.Issue;
import com.vulntrack.model.User;
import com.vulntrack.repository.UserRepository;
import com.vulntrack.service.CvssService;
import com.vulntrack.service.IssueService;
import com.vulntrack.utils.SecurityUtils;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/issues")
public class IssueController {
    private final IssueService service;
    private final CvssService cvssService;
    private final UserRepository userRepository;

    @GetMapping("/me")
    public String getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        return "Logged in as: " + userDetails.getUsername();
    }

    @PostMapping
    public IssueResponse createIssue(
        @Valid @RequestBody CreateIssueRequest issueRequest
    ) {

        User owner = userRepository.findById(SecurityUtils.getCurrentUser().id())
            .orElseThrow(() -> new BadErrorRequestException("User not found"));

        String description = issueRequest.description();
        String title = issueRequest.title();

        Double finalCvss = issueRequest.cvssScore();
        if (issueRequest.cvssVector() != null && !issueRequest.cvssVector().isEmpty()) {
            try {
                finalCvss = cvssService.calculateBaseScore(issueRequest.cvssVector());
                System.out.println("==========> " + finalCvss + " " + issueRequest.cvssVector());
            } catch (Exception e) {
                throw new BadErrorRequestException("Invalid CVSS Vector format");
            }
        }

        Issue issue = Issue.builder()
            .title(title)
            .description(description)
            .cvssScore(finalCvss)
            .severity(issueRequest.severity())
            .cweId(issueRequest.cweId())
            .owner(owner)
            .build();

        return IssueResponse.from(this.service.create(issue, owner));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@issueService.isOwner(#id, authentication.principal)")
    @AuditLog(action = "ACCESS_ISSUE", resource = "ISSUE", id = "#id")
    public IssueResponse findById(@PathVariable long id) {
        Issue issue = service.getById(id);

        return IssueResponse.from(issue);
    }

    @PutMapping("/{id}")
    public IssueResponse updateIssue(
        @PathVariable Long id,
        @Valid @RequestBody UpdateIssueRequest request
    ) {

        User owner = userRepository.findById(SecurityUtils.getCurrentUser().id())
            .orElseThrow(() -> new BadErrorRequestException("User not found"));
        return IssueResponse.from(this.service.updateIssue(id, owner, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteIssue(
        @PathVariable Long id
    ) {
        User owner = userRepository.findById(SecurityUtils.getCurrentUser().id())
            .orElseThrow(() -> new BadErrorRequestException("User not found"));

        this.service.deleteIssue(id, owner);
    }

    @PatchMapping("/{id}")
    public IssueResponse patchIssue(
        @PathVariable Long id,
        @Valid @RequestBody IssuePatchRequest request
    ) throws Exception {
        User owner = userRepository.findById(SecurityUtils.getCurrentUser().id())
            .orElseThrow(() -> new BadErrorRequestException("User not found"));

        return this.service.patchIssue(id, owner, request);
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
