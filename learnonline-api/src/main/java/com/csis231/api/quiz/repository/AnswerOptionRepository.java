package com.csis231.api.quiz.repository;

import com.csis231.api.quiz.model.AnswerOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnswerOptionRepository extends JpaRepository<AnswerOption, Long> {

    List<AnswerOption> findByQuestion_IdIn(List<Long> questionIds);
}
