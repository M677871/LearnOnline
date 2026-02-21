package com.csis231.api.quiz;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizResultRepository extends JpaRepository<QuizResult, Long> {
    /**
     * Retrieves the latest five results for a student ordered by completion time descending.
     *
     * @param studentId the student identifier
     * @return list of {@link QuizResult}
     */
    List<QuizResult> findTop5ByStudent_IdOrderByCompletedAtDesc(Long studentId);
    /**
     * Retrieves all results for a specific quiz.
     *
     * @param quizId the quiz identifier
     * @return list of {@link QuizResult}
     */
    List<QuizResult> findByQuiz_Id(Long quizId);
    List<QuizResult> findByQuiz_Course_Id(Long courseId);

    /**
     * Retrieves the latest result for a student on a given quiz.
     *
     * @param quizId    the quiz identifier
     * @param studentId the student identifier
     * @return an optional containing the latest {@link QuizResult} if present
     */
    java.util.Optional<QuizResult> findTop1ByQuiz_IdAndStudent_IdOrderByCompletedAtDesc(Long quizId, Long studentId);
}
