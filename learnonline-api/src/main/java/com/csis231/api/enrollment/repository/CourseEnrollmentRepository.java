package com.csis231.api.enrollment.repository;

import com.csis231.api.enrollment.model.CourseEnrollment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseEnrollmentRepository extends JpaRepository<CourseEnrollment, Long> {

    boolean existsByStudent_IdAndCourse_Id(Long studentId, Long courseId);

    long countByCourse_Id(Long courseId);

    @EntityGraph(attributePaths = {"student", "course", "course.instructor"})
    List<CourseEnrollment> findByStudent_Id(Long studentId);

    @EntityGraph(attributePaths = {"student", "course"})
    List<CourseEnrollment> findByCourse_Id(Long courseId);
}
