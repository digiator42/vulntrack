package com.vulntrack.model;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "issues")
public class Issue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime createdAt;

    private String title;
    private String description;
    private Integer severity;
    private Long ownerId;

    @ManyToOne
    private User createdBy;

    @Enumerated(EnumType.STRING)
    private IssueStatus status;

    private boolean isUpdated;
    private boolean adminCreated;

    public Issue() {}

    public Issue(Long ownerId) { this.ownerId = ownerId; }

    public Issue(Long id, String title, String description, Integer severity, Long ownerId) {
        this.id = id;
        this.createdAt = LocalDateTime.now();
        this.title = title;
        this.description = description;
        this.severity = severity;
        this.ownerId = ownerId;
    }

    public Long getId() { return id; }

    public void setId(Long id) { this.id = id; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getTitle() { return title; }

    public void setTitle(String title) {
        this.title = title;
        this.setUpdated(true);
    }

    public String getDescription() { return description; }

    public void setDescription(String description) {
        this.description = description;
        this.setUpdated(true);
    }

    public User getCreatedBy() { return createdBy; }

    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }

    public Integer getSeverity() { return severity; }

    public void setSeverity(Integer severity) {
        this.severity = severity;
        this.setUpdated(true);
    }

    public Long getOwnerId() { return ownerId; }

    public IssueStatus getStatus() { return status; }

    public void setStatus(IssueStatus status) {
        this.status = status;
        this.setUpdated(true);
    }

    public boolean isUpdated() { return isUpdated; }

    public void setUpdated(boolean isUpdated) { this.isUpdated = isUpdated; }

    public boolean isAdminCreated() { return adminCreated; }

    public void setAdminCreated(boolean adminCreated) { this.adminCreated = adminCreated; }

    @Override
    public String toString() {
        return "Issue [id=" + id + ", createdAt=" + createdAt + ", title=" + title
            + ", description=" + description
            + ", severity=" + severity + ", ownerId=" + ownerId + ", status=" + status
            + ", isUpdated=" + isUpdated
            + ", getId()=" + getId() + "]";
    }
}
