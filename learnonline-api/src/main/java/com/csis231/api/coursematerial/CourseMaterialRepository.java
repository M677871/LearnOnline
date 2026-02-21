package com.csis231.api.coursematerial;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for {@link CourseMaterial} entities.
 */
@Repository
public interface CourseMaterialRepository extends JpaRepository<CourseMaterial, Long> {
    /**
     * Finds materials belonging to a given course.
     *
     * @param courseId the course identifier
     * @return list of materials for the course
     */
    List<CourseMaterial> findByCourse_Id(Long courseId);
}
