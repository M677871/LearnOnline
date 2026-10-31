package com.csis231.api.quiz.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record QuizQuestionRequest(
        @NotBlank String questionText,
        @NotEmpty List<@Valid AnswerCreateRequest> answers
) {
}
