package com.vulntrack.model;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.vulntrack.enums.IssueStatus;
import com.vulntrack.enums.Severity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "issues")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Issue {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private Long ownerId;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    @Builder.Default
    private boolean isUpdated = false;

    @Enumerated(EnumType.ORDINAL)
    private Severity severity; // LOW, MEDIUM, HIGH, CRITICAL

    private Double cvssScore; // 0.0 - 10.0

    private String cweId; // e.g., "CWE-79" for XSS

    private String cveId; // e.g., "CVE-2023-1234"

    @Enumerated(EnumType.STRING)
    private IssueStatus status; // OPEN, IN_PROGRESS, RESOLVED, CLOSED

    private String scannerSource; // "OWASP ZAP", "Snyk", "Manual Pentest"

    public Issue(String title, String description, Severity severity, Long ownerId) {
        this.title = title;
        this.description = description;
        this.severity = severity;
        this.ownerId = ownerId;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private User assignee; // Who is fixing it?

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (status == null)
            status = IssueStatus.OPEN;
    }

    @Override
    public String toString() {
        return "Issue [id=" + id + ", createdAt=" + createdAt + ", title=" + title
            + ", description=" + description
            + ", severity=" + severity + ", ownerId=" + ownerId + ", status=" + status
            + ", isUpdated=" + isUpdated
            + ", getId()=" + getId() + "]";
    }
}
