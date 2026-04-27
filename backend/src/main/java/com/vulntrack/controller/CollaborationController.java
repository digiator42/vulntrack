package com.vulntrack.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.vulntrack.dto.ActivityResponse;
import com.vulntrack.dto.UserResponse;
import com.vulntrack.model.Comment;
import com.vulntrack.service.CollaborationService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/collaborate/{issueId}")
@RequiredArgsConstructor
public class CollaborationController {

    private final CollaborationService collabService;

    public record CommentResponse(
        @JsonProperty("id") Long id,
        @JsonProperty("content") String content,
        UserResponse author
    ) {
        public static CommentResponse from(Comment comment) {
            return new CommentResponse(comment.getId(), comment.getContent(), UserResponse.from(comment.getAuthor()));
        }
    }

    @PostMapping("/comments")
    public CommentResponse addComment(
        @PathVariable Long issueId,
        @RequestParam String content
    ) {
        System.out.println("Received content: " + content);
        return CommentResponse.from(collabService.addComment(issueId, content));
    }

    @GetMapping("/comments")
    public List<CommentResponse> getComments(@PathVariable Long issueId) {
        System.out.println("=============>> " + issueId);
        return collabService.getComments(issueId);
    }

    @GetMapping("/activity")
    public List<ActivityResponse> getActivity(@PathVariable Long issueId) {
        return collabService.getActivityLog(issueId);
    }
}