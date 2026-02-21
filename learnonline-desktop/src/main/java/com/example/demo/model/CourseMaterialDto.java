package com.example.demo.model;

import java.time.Instant;

/**
 * DTO representing course material.
 *
 * <p>Used by the course detail/editor views to list downloadable links,
 * documents, or other resources attached to a course.</p>
 */
public class CourseMaterialDto {
    private Long id;
    private Long courseId;
    private String title;
    private String materialType;
    private String url;
    private String metadata;
    private Instant createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMaterialType() { return materialType; }
    public void setMaterialType(String materialType) { this.materialType = materialType; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
