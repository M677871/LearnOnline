package com.csis231.api.enrollment.mapper;

import com.csis231.api.enrollment.dto.EnrollmentResponse;
import com.csis231.api.enrollment.model.CourseEnrollment;

public final class EnrollmentMapper {

    private EnrollmentMapper() {
    }

    public static EnrollmentResponse toDto(CourseEnrollment enrollment) {
        return new EnrollmentResponse(
                enrollment.getId(),
                enrollment.getStudent().getId(),
                enrollment.getStudent().getUsername(),
                enrollment.getStudent().getEmail(),
                enrollment.getCourse().getId(),
                enrollment.getCourse().getTitle(),
                enrollment.getStatus(),
                enrollment.getEnrolledAt()
        );
    }
}
