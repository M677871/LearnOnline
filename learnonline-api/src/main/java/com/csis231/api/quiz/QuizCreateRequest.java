package com.csis231.api.quiz;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating a quiz.
 */
public record QuizCreateRequest(
        @NotNull Long courseId,
        @NotBlank @Size(min = 3, max = 200) String name,
        @Size(max = 2000) String description
) {}
