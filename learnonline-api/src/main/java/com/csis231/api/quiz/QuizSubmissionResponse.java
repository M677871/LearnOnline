package com.csis231.api.quiz;

import java.time.Instant;

/**
 * Response returned after a student submits a quiz.
 */
public record QuizSubmissionResponse(
        Long quizId,
        Long studentUserId,
        int score,
        int totalQuestions,
        double percentage,
        Instant completedAt
) {}
