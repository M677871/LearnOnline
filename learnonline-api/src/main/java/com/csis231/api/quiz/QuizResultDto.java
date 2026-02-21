package com.csis231.api.quiz;

import java.time.Instant;

/**
 * DTO summarizing a stored quiz result.
 */
public record QuizResultDto(
        Long id,
        Long quizId,
        Long studentUserId,
        int score,
        int totalQuestions,
        Instant completedAt
) {}
