package com.csis231.api.course;

import java.time.Instant;

/**
 * Data transfer object representing a course in list views.
 */
public record CourseDto(
        Long id,
        String title,
        String description,
        Long instructorUserId,
        String instructorName,
        Long categoryId,
        Boolean published,
        Instant createdAt,
        Instant updatedAt
) {}
