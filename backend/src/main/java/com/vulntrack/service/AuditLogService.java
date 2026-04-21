package com.vulntrack.service;

import com.vulntrack.model.AuditLog;
import com.vulntrack.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {
    private final AuditLogRepository repository;

    @Async("auditExecutor")
    public void persistAuditLog(
        String principal,
        String action,
        String resource,
        String resourceId,
        String status
    ) {
        try {
            AuditLog entity = AuditLog.builder()
                .timestamp(Instant.now())
                .principal(principal)
                .action(action)
                .resource(resource)
                .resourceId(resourceId)
                .status(status)
                .build();
            repository.save(entity);
        } catch (Exception e) {
            log.error(
                "[AUDIT] Failed to persist log: action={}, resource={}, error={}", action, resource,
                e.getMessage(), e
            );
        }
    }
}