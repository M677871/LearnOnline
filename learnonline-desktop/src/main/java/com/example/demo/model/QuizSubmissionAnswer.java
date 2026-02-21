package com.example.demo.model;

/**
 * Single answer choice used when submitting a quiz.
 *
 * <p>Maps a question id to the selected answer option id.</p>
 */
public class QuizSubmissionAnswer {
    private Long questionId;
    private Long answerId;

    public QuizSubmissionAnswer() {}
    public QuizSubmissionAnswer(Long questionId, Long answerId) {
        this.questionId = questionId;
        this.answerId = answerId;
    }

    public Long getQuestionId() { return questionId; }
    public void setQuestionId(Long questionId) { this.questionId = questionId; }

    public Long getAnswerId() { return answerId; }
    public void setAnswerId(Long answerId) { this.answerId = answerId; }
}
