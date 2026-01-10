package com.megastar.vulntrack.model;

public class Issue {
    private Long id;
    private String title;
    private String description;
    private Long ownerId;

    public Issue() {}

    public Issue(Long id, String title, String description, Long ownerId) {
        this.id = id;
        this.title = title;
        this.description = description;
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

    public long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(long ownerId) {
        this.ownerId = ownerId;
    }
}
