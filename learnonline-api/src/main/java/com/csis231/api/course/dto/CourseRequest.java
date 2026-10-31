package com.csis231.api.course.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CourseRequest(
        @NotBlank @Size(min = 3, max = 200) String title,
        @Size(max = 2000) String description,
        Long categoryId,
        Boolean published
) {
}
