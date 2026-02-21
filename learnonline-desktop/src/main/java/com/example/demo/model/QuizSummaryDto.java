package com.example.demo.model;

import java.time.Instant;

/**
 * Lightweight quiz summary shown in course detail and dashboards.
 *
 * <p>Contains enough metadata to list quizzes without loading full questions.</p>
 */
public class QuizSummaryDto {
    private Long id;
    private Long courseId;
    private String name;
    private String description;
    private int questionCount;
    private Instant createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getQuestionCount() { return questionCount; }
    public void setQuestionCount(int questionCount) { this.questionCount = questionCount; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
