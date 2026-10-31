package com.csis231.api.quiz.dto;

/**
 * An option as shown to quiz takers: deliberately without the correctness flag.
 */
public record AnswerOptionDto(
        Long id,
        String answerText
) {
}
