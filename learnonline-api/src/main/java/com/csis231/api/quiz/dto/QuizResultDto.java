package com.csis231.api.quiz.dto;

import java.time.Instant;

public record QuizResultDto(
        Long id,
        Long quizId,
        Long studentUserId,
        int score,
        int totalQuestions,
        Instant completedAt
) {
}
