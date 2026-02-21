package com.example.demo.model;

/**
 * Represents a single answer option when creating questions.
 *
 * <p>Used by instructors in the course editor when building quiz questions.
 * Each option carries display text and whether it is the correct answer.</p>
 */
public class AnswerCreateRequest {
    private String answerText;
    private boolean correct;

    public AnswerCreateRequest() {}
    public AnswerCreateRequest(String answerText, boolean correct) {
        this.answerText = answerText;
        this.correct = correct;
    }

    public String getAnswerText() { return answerText; }
    public void setAnswerText(String answerText) { this.answerText = answerText; }

    public boolean isCorrect() { return correct; }
    public void setCorrect(boolean correct) { this.correct = correct; }
}
