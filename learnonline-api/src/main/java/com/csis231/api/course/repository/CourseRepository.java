package com.csis231.api.course.repository;

import com.csis231.api.common.exception.ResourceNotFoundException;
import com.csis231.api.course.model.Course;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface CourseRepository extends JpaRepository<Course, Long>, JpaSpecificationExecutor<Course> {

    /** Fetches the instructor in the same query: every catalog row shows the instructor's name. */
    @Override
    @EntityGraph(attributePaths = "instructor")
    Page<Course> findAll(Specification<Course> spec, Pageable pageable);

    List<Course> findByInstructor_Id(Long instructorId);

    default Course getOrThrow(Long id) {
        return findById(id).orElseThrow(() -> new ResourceNotFoundException("Course not found: " + id));
    }
}
