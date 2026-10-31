package com.csis231.api.quiz.dto;

import java.util.List;

public record QuizQuestionDto(
        Long id,
        String questionText,
        List<AnswerOptionDto> options
) {
}
