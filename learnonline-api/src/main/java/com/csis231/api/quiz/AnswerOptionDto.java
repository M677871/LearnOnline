package com.csis231.api.quiz;

/**
 * DTO representing a single answer option without exposing correctness.
 */
public record AnswerOptionDto(
        Long id,
        String answerText
) {}
