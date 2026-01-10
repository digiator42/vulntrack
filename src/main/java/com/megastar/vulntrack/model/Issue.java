package com.megastar.vulntrack.model;

public class Issue {
    private long id;
    private String title;
    private String description;
    private long ownerId;

    public Issue() {}

    public Issue(long id, String title, String description, long ownerId) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.ownerId = ownerId;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
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
