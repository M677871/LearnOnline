package com.csis231.api.quiz;

import java.util.List;

/**
 * DTO representing a quiz question and its answer options.
 */
public record QuizQuestionDto(
        Long id,
        String questionText,
        List<AnswerOptionDto> options
) {}
