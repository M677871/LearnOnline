package com.example.demo.model;

import java.util.List;

/**
 * Represents a question + answers when creating a quiz.
 *
 * <p>Each answer should mark exactly one as correct; if multiple are marked,
 * the backend may enforce validation.</p>
 */
public class QuizQuestionRequest {
    private String questionText;
    private List<AnswerCreateRequest> answers;

    public QuizQuestionRequest() {}

    public QuizQuestionRequest(String questionText, List<AnswerCreateRequest> answers) {
        this.questionText = questionText;
        this.answers = answers;
    }

    public String getQuestionText() { return questionText; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }

    public List<AnswerCreateRequest> getAnswers() { return answers; }
    public void setAnswers(List<AnswerCreateRequest> answers) { this.answers = answers; }
}
