package com.csis231.api.course;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data repository for {@link Course} entities.
 */
@Repository
public interface CourseRepository extends JpaRepository<Course, Long>, JpaSpecificationExecutor<Course> {

    /**
     * Retrieves courses taught by a specific instructor using pagination.
     *
     * @param instructorId the instructor's user id
     * @param pageable     paging information
     * @return a page of {@link Course} entities owned by the instructor
     */
    Page<Course> findByInstructor_Id(Long instructorId, Pageable pageable);

    /**
     * Retrieves all courses taught by a specific instructor.
     *
     * @param instructorId the instructor's user id
     * @return a list of {@link Course} entities owned by the instructor
     */
    List<Course> findByInstructor_Id(Long instructorId);
}
