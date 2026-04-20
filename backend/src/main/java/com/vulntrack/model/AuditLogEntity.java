package com.vulntrack.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "audit_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Instant timestamp;

    @Column(nullable = false)
    private String principal; // username or "anonymous"

    @Column(nullable = false)
    private String action;    // "ACCESS_ISSUE", "PATCH_ISSUE", etc.

    @Column(nullable = false)
    private String resource;  // "ISSUE", "USER", etc.

    @Column(name = "resource_id")
    private String resourceId;

    @Column(nullable = false)
    private String status;    // "SUCCESS" or "FAILURE"
}