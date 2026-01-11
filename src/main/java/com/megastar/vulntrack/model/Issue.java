package com.megastar.vulntrack.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class Issue {
    private Long id;
    @NotBlank(message = "Title is required")
    private String title;
    private String description;
    @NotNull(message = "Severity is required")
    private Integer severity;
    private Long ownerId;

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }


    public Issue() {}
    public Issue(Long id, String title, String description, Integer severity, Long ownerId) {
        this.id = id;
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
    public Integer getSeverity() {
        return severity;
    }
    public void setSeverity(Integer severity) {
        this.severity = severity;
    }

    public long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(long ownerId) {
        this.ownerId = ownerId;
    }

    @Override
    public String toString() {
        return "Issue{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", ownerId=" + ownerId +
                '}';
    }
}
