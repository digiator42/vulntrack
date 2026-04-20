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
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "issues")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
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

    public Issue(Long ownerId) { this.ownerId = ownerId; }

    public Issue(Long id, String title, String description, Integer severity, Long ownerId) {
        this.id = id;
        this.createdAt = LocalDateTime.now();
        this.title = title;
        this.description = description;
        this.severity = severity;
        this.ownerId = ownerId;
    }

    public void setTitle(String title) {
        this.title = title;
        this.setUpdated(true);
    }

    public String getDescription() { return description; }

    public void setDescription(String description) {
        this.description = description;
        this.setUpdated(true);
    }

    public Integer getSeverity() { return severity; }

    public void setSeverity(Integer severity) {
        this.severity = severity;
        this.setUpdated(true);
    }

    public void setStatus(IssueStatus status) {
        this.status = status;
        this.setUpdated(true);
    }

    public boolean isUpdated() { return isUpdated; }

    public boolean isAdminCreated() { return adminCreated; }

    @Override
    public String toString() {
        return "Issue [id=" + id + ", createdAt=" + createdAt + ", title=" + title
            + ", description=" + description
            + ", severity=" + severity + ", ownerId=" + ownerId + ", status=" + status
            + ", isUpdated=" + isUpdated
            + ", getId()=" + getId() + "]";
    }
}
