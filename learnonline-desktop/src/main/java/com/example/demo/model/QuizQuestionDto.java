package com.example.demo.model;

import java.util.List;

/**
 * Quiz question with its answer options (without correctness flags).
 */
public class QuizQuestionDto {
    private Long id;
    private String questionText;
    private List<AnswerOptionDto> options;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getQuestionText() { return questionText; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }

    public List<AnswerOptionDto> getOptions() { return options; }
    public void setOptions(List<AnswerOptionDto> options) { this.options = options; }
}
