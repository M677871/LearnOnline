package com.csis231.api.quiz;

import java.util.List;

/**
 * Full quiz definition returned to students (without correctness flags).
 */
public record QuizDetailDto(
        Long id,
        Long courseId,
        String name,
        String description,
        List<QuizQuestionDto> questions
) {}
