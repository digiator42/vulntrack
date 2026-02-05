package com.megastar.vulntrack.model;

import java.time.LocalDate;

public class Issue {
    private Long id;
    private String createdAt;
    private String title;
    private String description;
    private Integer severity;
    private final Long ownerId;
    private IssueStatus status;
    private boolean isUpdated;

    public Issue(Long id, String title, String description, Integer severity, Long ownerId) {
        this.id = id;
        this.createdAt = String.valueOf(LocalDate.now());
        this.title = title;
        this.description = description;
        this.severity = severity;
        this.ownerId = ownerId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
        this.setUpdated(true);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
        this.setUpdated(true);
    }

    public Integer getSeverity() {
        return severity;
    }

    public void setSeverity(Integer severity) {
        this.severity = severity;
        this.setUpdated(true);
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public IssueStatus getStatus() {
        return status;
    }

    public void setStatus(IssueStatus status) {
        this.status = status;
        this.setUpdated(true);
    }

    public boolean isUpdated() {
        return isUpdated;
    }

    public void setUpdated(boolean isUpdated) {
        this.isUpdated = isUpdated;
    }

    @Override
    public String toString() {
        return "Issue [id=" + id + ", createdAt=" + createdAt + ", title=" + title + ", description=" + description
                + ", severity=" + severity + ", ownerId=" + ownerId + ", status=" + status + ", isUpdated=" + isUpdated
                + ", getId()=" + getId() + "]";
    }
}
