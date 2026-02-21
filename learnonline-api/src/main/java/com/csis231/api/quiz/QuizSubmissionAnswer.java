package com.csis231.api.quiz;

import jakarta.validation.constraints.NotNull;

/**
 * DTO carrying an answer chosen by a student for a question.
 */
public record QuizSubmissionAnswer(
        @NotNull Long questionId,
        @NotNull Long answerId
) {}
