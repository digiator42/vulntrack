package com.vulntrack.dto;

import java.time.LocalDateTime;

import com.vulntrack.model.ActivityLog;

public record ActivityResponse(
    Long id,
    String action,
    String performedBy, // Username instead of User object
    LocalDateTime timestamp
) {
    public static ActivityResponse from(ActivityLog log) {
        return new ActivityResponse(
            log.getId(),
            log.getAction(),
            log.getPerformedBy().getUsername(),
            log.getCreatedAt()
        );
    }
}