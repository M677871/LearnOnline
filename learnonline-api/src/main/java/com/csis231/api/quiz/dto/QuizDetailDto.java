package com.csis231.api.quiz.dto;

import java.util.List;

/**
 * Quiz content for taking it. Options carry no correctness flag, so answers stay hidden.
 */
public record QuizDetailDto(
        Long id,
        Long courseId,
        String name,
        String description,
        List<QuizQuestionDto> questions
) {
}
