package com.csis231.api.course;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for creating or updating a course.
 */
public record CourseRequest(
        @NotBlank @Size(min = 3, max = 200) String title,
        @Size(max = 2000) String description,
        Long categoryId,
        Boolean published
) {}
