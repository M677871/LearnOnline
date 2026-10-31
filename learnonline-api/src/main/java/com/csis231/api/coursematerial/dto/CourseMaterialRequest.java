package com.csis231.api.coursematerial.dto;

import jakarta.validation.constraints.NotBlank;

public record CourseMaterialRequest(
        @NotBlank String title,
        String materialType,
        String url,
        String metadata
) {
}
