package com.csis231.api.quiz.mapper;

import com.csis231.api.quiz.dto.AnswerOptionDto;
import com.csis231.api.quiz.dto.QuizDetailDto;
import com.csis231.api.quiz.dto.QuizQuestionDto;
import com.csis231.api.quiz.dto.QuizResultDto;
import com.csis231.api.quiz.dto.QuizSummaryDto;
import com.csis231.api.quiz.model.AnswerOption;
import com.csis231.api.quiz.model.Quiz;
import com.csis231.api.quiz.model.QuizQuestion;
import com.csis231.api.quiz.model.QuizResult;

import java.util.List;
import java.util.Map;

public final class QuizMapper {

    private QuizMapper() {
    }

    public static QuizSummaryDto toSummaryDto(Quiz quiz, int questionCount) {
        return new QuizSummaryDto(
                quiz.getId(),
                quiz.getCourse().getId(),
                quiz.getName(),
                quiz.getDescription(),
                questionCount,
                quiz.getCreatedAt()
        );
    }

    public static QuizDetailDto toDetailDto(Quiz quiz,
                                            List<QuizQuestion> questions,
                                            Map<Long, List<AnswerOption>> optionsByQuestion) {
        List<QuizQuestionDto> questionDtos = questions.stream()
                .map(question -> toQuestionDto(question, optionsByQuestion.getOrDefault(question.getId(), List.of())))
                .toList();
        return new QuizDetailDto(
                quiz.getId(),
                quiz.getCourse().getId(),
                quiz.getName(),
                quiz.getDescription(),
                questionDtos
        );
    }

    public static QuizResultDto toResultDto(QuizResult result) {
        return new QuizResultDto(
                result.getId(),
                result.getQuiz().getId(),
                result.getStudent().getId(),
                result.getScore(),
                result.getTotalQuestions(),
                result.getCompletedAt()
        );
    }

    private static QuizQuestionDto toQuestionDto(QuizQuestion question, List<AnswerOption> options) {
        List<AnswerOptionDto> optionDtos = options.stream()
                .map(option -> new AnswerOptionDto(option.getId(), option.getAnswerText()))
                .toList();
        return new QuizQuestionDto(question.getId(), question.getQuestionText(), optionDtos);
    }
}
