package com.csis231.api.quiz;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Request DTO for submitting a quiz.
 */
public record QuizSubmissionRequest(
        @NotEmpty List<@Valid QuizSubmissionAnswer> answers
) {}
