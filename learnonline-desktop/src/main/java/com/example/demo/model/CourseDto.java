package com.example.demo.model;

import java.time.Instant;

/**
 * Lightweight course projection used across dashboards, catalog, and lists.
 *
 * <p>Includes author, publish status, timestamps, and description so the UI
 * can show summary cards or table rows without fetching full details.</p>
 */
public class CourseDto {
    private Long id;
    private String title;
    private String description;
    private Long instructorUserId;
    private String instructorName;
    private Long categoryId;
    private Boolean published;
    private Instant createdAt;
    private Instant updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Long getInstructorUserId() { return instructorUserId; }
    public void setInstructorUserId(Long instructorUserId) { this.instructorUserId = instructorUserId; }

    public String getInstructorName() { return instructorName; }
    public void setInstructorName(String instructorName) { this.instructorName = instructorName; }

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public Boolean getPublished() { return published; }
    public void setPublished(Boolean published) { this.published = published; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
