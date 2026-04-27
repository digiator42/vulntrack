package com.vulntrack.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;

public record CommentRequest(
    @NotBlank(message = "Comment content cannot be empty")
    @JsonProperty("content")
    String content
) {}