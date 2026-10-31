package com.csis231.api.enrollment.dto;

import jakarta.validation.constraints.NotNull;

/**
 * @param studentUserId the user to enroll; when omitted, the authenticated user is enrolled
 */
public record EnrollmentRequest(
        Long studentUserId,
        @NotNull Long courseId
) {
}
