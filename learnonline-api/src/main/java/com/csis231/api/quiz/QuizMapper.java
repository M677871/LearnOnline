package com.csis231.api.quiz;

import java.util.List;

/**
 * Mapper utilities for quiz-related DTOs.
 */
public final class QuizMapper {
    private QuizMapper() {}

    /**
     * Maps a {@link Quiz} to a summary DTO with zero questions counted.
     *
     * @param quiz the quiz entity
     * @return a {@link QuizSummaryDto} or {@code null} if input is null
     */
    public static QuizSummaryDto toSummaryDto(Quiz quiz) {
        return toSummaryDto(quiz, 0);
    }

    /**
     * Maps a {@link Quiz} to a summary DTO with an explicit question count.
     *
     * @param quiz          the quiz entity
     * @param questionCount the number of questions in the quiz
     * @return a {@link QuizSummaryDto} or {@code null} if input is null
     */
    public static QuizSummaryDto toSummaryDto(Quiz quiz, int questionCount) {
        if (quiz == null) return null;
        return new QuizSummaryDto(
                quiz.getId(),
                quiz.getCourse() != null ? quiz.getCourse().getId() : null,
                quiz.getName(),
                quiz.getDescription(),
                questionCount,
                quiz.getCreatedAt()
        );
    }

    /**
     * Maps a {@link Quiz} and its questions to a detailed DTO.
     *
     * @param quiz      the quiz entity
     * @param questions the list of question DTOs
     * @return a {@link QuizDetailDto} or {@code null} if quiz is null
     */
    public static QuizDetailDto toDetailDto(Quiz quiz, List<QuizQuestionDto> questions) {
        if (quiz == null) return null;
        return new QuizDetailDto(
                quiz.getId(),
                quiz.getCourse() != null ? quiz.getCourse().getId() : null,
                quiz.getName(),
                quiz.getDescription(),
                questions
        );
    }

    /**
     * Maps a {@link QuizResult} to its DTO representation.
     *
     * @param result the quiz result entity
     * @return a {@link QuizResultDto} or {@code null} if input is null
     */
    public static QuizResultDto toResultDto(QuizResult result) {
        if (result == null) return null;
        return new QuizResultDto(
                result.getId(),
                result.getQuiz() != null ? result.getQuiz().getId() : null,
                result.getStudent() != null ? result.getStudent().getId() : null,
                result.getScore(),
                result.getTotalQuestions(),
                result.getCompletedAt()
        );
    }
}
