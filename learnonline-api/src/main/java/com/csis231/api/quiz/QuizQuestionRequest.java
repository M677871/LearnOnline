package com.csis231.api.quiz;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Request DTO for creating quiz questions and their options.
 */
public record QuizQuestionRequest(
        @NotBlank String questionText,
        @NotEmpty List<@Valid AnswerCreateRequest> answers
) {}
