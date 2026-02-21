package com.example.demo.model;

/**
 * Request body for creating a quiz.
 *
 * <p>Includes the owning course id plus quiz title/description.</p>
 */
public class QuizCreateRequest {
    private Long courseId;
    private String name;
    private String description;

    public QuizCreateRequest() {}

    public QuizCreateRequest(Long courseId, String name, String description) {
        this.courseId = courseId;
        this.name = name;
        this.description = description;
    }

    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
