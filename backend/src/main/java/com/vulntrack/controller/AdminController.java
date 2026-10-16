package com.vulntrack.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.vulntrack.dto.IssueResponse;
import com.vulntrack.dto.UserResponse;
import com.vulntrack.enums.Role;
import com.vulntrack.model.Issue;
import com.vulntrack.model.User;
import com.vulntrack.repository.UserRepository;
import com.vulntrack.service.IssueService;
import com.vulntrack.service.JwtService;

import io.swagger.v3.oas.annotations.parameters.RequestBody;
import lombok.RequiredArgsConstructor;

import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')") // Only admins can access this controller
@RequiredArgsConstructor
public class AdminController {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final IssueService issueService;

    public record UpdateRoleRequest(
        @NotNull Role newRole
    ) {
    }

    /**
     * Update the role of a user. PATCH /api/admin/users/{userId}/role
     */
    @PatchMapping("/users/{userId}/role")
    public void updateUserRole(@PathVariable Long userId, @RequestParam String newRole) {
        User user = userRepository.findById(userId).orElseThrow();
        user.setRole(Role.valueOf(newRole));
        userRepository.save(user);
    }

    /**
     * Assign an issue to a developer. POST
     * /api/admin/issues/{issueId}/assign?assigneeId={userId}
     */
    @PostMapping("/issues/{issueId}/assign")
    public IssueResponse assignIssue(
        @PathVariable Long issueId,
        @RequestParam Long assigneeId
    ) {

        Issue updatedIssue = issueService.assignIssue(issueId, assigneeId);
        return IssueResponse.withAsignee(updatedIssue);
    }

    /**
     * Unassign an issue (remove assignee). DELETE
     * /api/admin/issues/{issueId}/assign
     */
    @DeleteMapping("/issues/{issueId}/assign")
    public IssueResponse unassignIssue(@PathVariable Long issueId) {
        Issue updatedIssue = issueService.unAssignIssue(issueId); // Handle null in service
        return IssueResponse.from(updatedIssue);
    }

    /**
     * Get all users (for admin dashboard). GET /api/admin/users
     */
    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers() {
        return ResponseEntity.ok(
            userRepository.findAll().stream()
                .map(UserResponse::from)
                .toList()
        );
    }

}
