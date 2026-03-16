package com.csis231.api.enrollment.dto;

import com.csis231.api.enrollment.model.CourseEnrollment;

import java.time.Instant;

/**
 * Response DTO representing a course enrollment.
 */
public record EnrollmentResponse(
        Long id,
        Long studentUserId,
        String studentUsername,
        String studentEmail,
        Long courseId,
        String courseTitle,
        CourseEnrollment.EnrollmentStatus status,
        Instant enrolledAt
) {}
