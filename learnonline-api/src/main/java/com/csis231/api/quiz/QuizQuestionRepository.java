package com.csis231.api.quiz;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, Long> {
    /**
     * Retrieves all questions for a given quiz.
     *
     * @param quizId the quiz identifier
     * @return list of {@link QuizQuestion} for the quiz
     */
    List<QuizQuestion> findByQuiz_Id(Long quizId);
}
