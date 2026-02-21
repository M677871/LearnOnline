package com.example.demo.model;

/**
 * Request payload for creating/updating courses.
 *
 * <p>Holds basic metadata (title, description), category selection, and
 * publish flag used by the course editor.</p>
 */
public class CourseRequest {
    private String title;
    private String description;
    private Long categoryId;
    private Boolean published;

    public CourseRequest() {}

    public CourseRequest(String title, String description, Long categoryId, Boolean published) {
        this.title = title;
        this.description = description;
        this.categoryId = categoryId;
        this.published = published;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public Boolean getPublished() { return published; }
    public void setPublished(Boolean published) { this.published = published; }
}
