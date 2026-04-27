package com.vulntrack.service;

import com.vulntrack.controller.CollaborationController.CommentResponse;
import com.vulntrack.dto.ActivityResponse;
import com.vulntrack.model.*;
import com.vulntrack.repository.*;
import com.vulntrack.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CollaborationService {

    private final CommentRepository commentRepo;
    private final ActivityLogRepository activityRepo;
    private final IssueRepository issueRepo;
    private final UserRepository userRepo;
    private final SimpMessagingTemplate messagingTemplate; // For WebSockets

    // Add Comment
    @Transactional
    public Comment addComment(Long issueId, String content) {
        SecurityUser user = SecurityUtils.getCurrentUser();
        Issue issue = issueRepo.findById(issueId)
            .orElseThrow(() -> new IllegalArgumentException("Issue not found"));

        Comment comment = Comment.builder()
            .content(content)
            .author(userRepo.findById(user.id()).orElseThrow())
            .issue(issue)
            .build();

        Comment saved = commentRepo.save(comment);

        // Log Activity, no need for now as the chat has logs already
        // logActivity(issue, user, "COMMENT_ADDED", null, content);

        CommentResponse response = CommentResponse.from(saved);

        // Broadcast to frontend
        messagingTemplate.convertAndSend(
            "/topic/issues/" + issueId,
            Map.of("type", "NEW_COMMENT", "data", response)
        );

        return saved;
    }

    // Log Status Change (Called from IssueService when status updates)
    public void logStatusChange(Issue issue, String oldStatus, String newStatus) {
        SecurityUser user = SecurityUtils.getCurrentUser();
        logActivity(issue, user, "STATUS_CHANGE", oldStatus, newStatus);

        // Broadcast
        messagingTemplate.convertAndSend(
            "/topic/issues/" + issue.getId(),
            Map.of("type", "STATUS_UPDATE", "old", oldStatus, "new", newStatus)
        );
    }

    public void logActivity(
        Issue issue,
        SecurityUser user,
        String action,
        String oldVal,
        String newVal
    ) {
        ActivityLog log = ActivityLog.builder()
            .action(action)
            .oldValue(oldVal)
            .newValue(newVal)
            .performedBy(userRepo.findById(user.id()).orElseThrow())
            .issue(issue)
            .build();
        activityRepo.save(log);
    }

    public List<CommentResponse> getComments(Long issueId) {
        return commentRepo.findByIssueIdOrderByCreatedAtAsc(issueId)
            .stream()
            .map(CommentResponse::from) // Converts Entity -> DTO
            .toList();
    }

    public List<ActivityResponse> getActivityLog(Long issueId) {
        return activityRepo.findByIssueIdOrderByCreatedAtDesc(issueId)
            .stream()
            .map(ActivityResponse::from) // Converts Entity -> DTO
            .toList();
    }
}