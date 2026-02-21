package com.example.demo.model;

import java.util.List;

/**
 * Full quiz definition returned to students.
 *
 * <p>Contains the quiz metadata plus the ordered list of questions and their
 * answer options.</p>
 */
public class QuizDetailDto {
    private Long id;
    private Long courseId;
    private String name;
    private String description;
    private List<QuizQuestionDto> questions;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<QuizQuestionDto> getQuestions() { return questions; }
    public void setQuestions(List<QuizQuestionDto> questions) { this.questions = questions; }
}
