package com.csis231.api.quiz;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for {@link Quiz}.
 */
@Repository
public interface QuizRepository extends JpaRepository<Quiz, Long> {
    /**
     * Retrieves quizzes belonging to a specific course.
     *
     * @param courseId the course identifier
     * @return list of {@link Quiz} entities for the course
     */
    List<Quiz> findByCourse_Id(Long courseId);
}
