package com.csis231.api.enrollment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link CourseEnrollment} entities.
 */
@Repository
public interface CourseEnrollmentRepository extends JpaRepository<CourseEnrollment, Long> {
    /**
     * Checks if an enrollment already exists for a given student and course.
     *
     * @param studentId the student identifier
     * @param courseId  the course identifier
     * @return true if the enrollment exists
     */
    boolean existsByStudent_IdAndCourse_Id(Long studentId, Long courseId);
    /**
     * Finds enrollments for a specific student.
     *
     * @param studentId the student identifier
     * @return list of enrollments for the student
     */
    List<CourseEnrollment> findByStudent_Id(Long studentId);
    /**
     * Finds enrollments for a specific course.
     *
     * @param courseId the course identifier
     * @return list of enrollments for the course
     */
    List<CourseEnrollment> findByCourse_Id(Long courseId);
    /**
     * Counts enrollments for a course.
     *
     * @param courseId the course identifier
     * @return the total number of enrollments
     */
    long countByCourse_Id(Long courseId);
    /**
     * Retrieves an enrollment by student and course identifiers.
     *
     * @param studentId the student identifier
     * @param courseId  the course identifier
     * @return an optional containing the enrollment if present
     */
    Optional<CourseEnrollment> findByStudent_IdAndCourse_Id(Long studentId, Long courseId);
}
