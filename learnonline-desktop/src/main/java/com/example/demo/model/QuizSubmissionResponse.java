package com.example.demo.model;

import java.time.Instant;

/**
 * Response returned after submitting a quiz.
 *
 * <p>Includes score breakdown and completion timestamp for confirmation and
 * dashboard summaries.</p>
 */
public class QuizSubmissionResponse {
    private Long quizId;
    private Long studentUserId;
    private int score;
    private int totalQuestions;
    private double percentage;
    private Instant completedAt;

    public Long getQuizId() { return quizId; }
    public void setQuizId(Long quizId) { this.quizId = quizId; }

    public Long getStudentUserId() { return studentUserId; }
    public void setStudentUserId(Long studentUserId) { this.studentUserId = studentUserId; }

    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }

    public int getTotalQuestions() { return totalQuestions; }
    public void setTotalQuestions(int totalQuestions) { this.totalQuestions = totalQuestions; }

    public double getPercentage() { return percentage; }
    public void setPercentage(double percentage) { this.percentage = percentage; }

    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
}
