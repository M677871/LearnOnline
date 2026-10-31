package com.csis231.api.quiz.dto;

import jakarta.validation.constraints.NotNull;

public record QuizSubmissionAnswer(
        @NotNull Long questionId,
        @NotNull Long answerId
) {
}
