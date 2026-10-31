package com.csis231.api.quiz.repository;

import com.csis231.api.quiz.model.QuizQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, Long> {

    List<QuizQuestion> findByQuiz_Id(Long quizId);

    int countByQuiz_Id(Long quizId);
}
