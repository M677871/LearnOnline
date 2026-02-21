package com.csis231.api.quiz;

import java.time.Instant;

/**
 * Lightweight quiz representation for course details.
 */
public record QuizSummaryDto(
        Long id,
        Long courseId,
        String name,
        String description,
        int questionCount,
        Instant createdAt
) {}
