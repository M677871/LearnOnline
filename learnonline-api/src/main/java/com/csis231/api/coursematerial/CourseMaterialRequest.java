package com.csis231.api.coursematerial;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for creating course materials.
 */
public record CourseMaterialRequest(
        @NotBlank String title,
        String materialType,
        String url,
        String metadata
) {}
