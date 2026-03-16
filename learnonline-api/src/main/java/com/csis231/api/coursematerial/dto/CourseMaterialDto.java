package com.csis231.api.coursematerial.dto;

import java.time.Instant;

/**
 * DTO representing course material in responses.
 */
public record CourseMaterialDto(
        Long id,
        Long courseId,
        String title,
        String materialType,
        String url,
        String metadata,
        Instant createdAt
) {}
