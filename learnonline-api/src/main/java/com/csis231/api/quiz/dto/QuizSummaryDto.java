package com.csis231.api.quiz.dto;

import java.time.Instant;

public record QuizSummaryDto(
        Long id,
        Long courseId,
        String name,
        String description,
        int questionCount,
        Instant createdAt
) {
}
