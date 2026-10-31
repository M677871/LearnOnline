package com.csis231.api.quiz.dto;

import java.time.Instant;

public record QuizSubmissionResponse(
        Long quizId,
        Long studentUserId,
        int score,
        int totalQuestions,
        double percentage,
        Instant completedAt
) {
}
