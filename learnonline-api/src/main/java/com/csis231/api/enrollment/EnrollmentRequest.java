package com.csis231.api.enrollment;

import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for enrolling a student into a course.
 *
 * <p>{@code studentUserId} is optional; if omitted the authenticated user
 * will be used.</p>
 */
public record EnrollmentRequest(
        Long studentUserId,
        @NotNull Long courseId
) {}
