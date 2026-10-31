package com.csis231.api.quiz.repository;

import com.csis231.api.common.exception.ResourceNotFoundException;
import com.csis231.api.quiz.model.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuizRepository extends JpaRepository<Quiz, Long> {

    List<Quiz> findByCourse_Id(Long courseId);

    int countByCourse_Id(Long courseId);

    default Quiz getOrThrow(Long id) {
        return findById(id).orElseThrow(() -> new ResourceNotFoundException("Quiz not found: " + id));
    }
}
