package com.vulntrack.dto;

import java.time.LocalDateTime;

import com.vulntrack.model.ActivityLog;

public record ActivityResponse(
    Long id,
    String action,
    UserResponse performedBy, // Username instead of User object
    String oldValue,
    String newValue,
    LocalDateTime timestamp
) {
    public static ActivityResponse from(ActivityLog log) {
        return new ActivityResponse(
            log.getId(),
            log.getAction(),
            UserResponse.from(log.getPerformedBy()),
            log.getOldValue(),
            log.getNewValue(),
            log.getCreatedAt()
        );
    }
}