package com.csis231.api.quiz.dto;

import jakarta.validation.constraints.NotBlank;

public record AnswerCreateRequest(
        @NotBlank String answerText,
        boolean correct
) {
}
