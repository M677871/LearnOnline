package com.csis231.api.quiz.repository;

import com.csis231.api.quiz.model.QuizResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QuizResultRepository extends JpaRepository<QuizResult, Long> {

    List<QuizResult> findByQuiz_Id(Long quizId);

    List<QuizResult> findByQuiz_Course_Id(Long courseId);

    List<QuizResult> findTop5ByStudent_IdOrderByCompletedAtDesc(Long studentId);

    Optional<QuizResult> findTop1ByQuiz_IdAndStudent_IdOrderByCompletedAtDesc(Long quizId, Long studentId);
}
