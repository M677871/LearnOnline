package com.csis231.api.quiz;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnswerOptionRepository extends JpaRepository<AnswerOption, Long> {
    /**
     * Finds answer options for a set of question identifiers.
     *
     * @param questionIds list of question ids
     * @return list of {@link AnswerOption} linked to the questions
     */
    List<AnswerOption> findByQuestion_IdIn(List<Long> questionIds);
}
