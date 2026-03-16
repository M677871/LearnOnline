package com.csis231.api.quiz.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO representing an answer option to be created.
 */
public record AnswerCreateRequest(
        @NotBlank String answerText,
        boolean correct
) {}
