package com.csis231.api.enrollment;

/**
 * Utility class converting enrollment entities to DTOs.
 */
public final class EnrollmentMapper {
    private EnrollmentMapper() {}

    /**
     * Converts a {@link CourseEnrollment} entity to its DTO representation.
     *
     * @param enrollment the enrollment entity to map
     * @return the mapped {@link EnrollmentResponse}, or {@code null} if input is null
     */
    public static EnrollmentResponse toDto(CourseEnrollment enrollment) {
        if (enrollment == null) return null;
        return new EnrollmentResponse(
                enrollment.getId(),
                enrollment.getStudent() != null ? enrollment.getStudent().getId() : null,
                enrollment.getStudent() != null ? enrollment.getStudent().getUsername() : null,
                enrollment.getStudent() != null ? enrollment.getStudent().getEmail() : null,
                enrollment.getCourse() != null ? enrollment.getCourse().getId() : null,
                enrollment.getCourse() != null ? enrollment.getCourse().getTitle() : null,
                enrollment.getStatus(),
                enrollment.getEnrolledAt()
        );
    }
}
