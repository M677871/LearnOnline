package com.csis231.api.dashboard.service;

import com.csis231.api.common.exception.ForbiddenException;
import com.csis231.api.course.model.Course;
import com.csis231.api.course.repository.CourseRepository;
import com.csis231.api.dashboard.dto.ChartPoint;
import com.csis231.api.quiz.model.QuizResult;
import com.csis231.api.quiz.repository.QuizRepository;
import com.csis231.api.quiz.repository.QuizResultRepository;
import com.csis231.api.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Read-only aggregates for the client's charts.
 */
@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final CourseRepository courseRepository;
    private final QuizRepository quizRepository;
    private final QuizResultRepository quizResultRepository;

    /**
     * Returns the average score of each quiz in a course as a percentage, weighted by the number of
     * questions per attempt. Quizzes nobody has attempted appear with an average of 0.
     */
    @Transactional(readOnly = true)
    public List<ChartPoint> quizAveragesForCourse(Long courseId, User actor) {
        Course course = courseRepository.getOrThrow(courseId);
        if (!course.isManagedBy(actor)) {
            throw new ForbiddenException("Not authorized to view course statistics");
        }

        Map<Long, List<QuizResult>> resultsByQuiz = quizResultRepository.findByQuiz_Course_Id(courseId).stream()
                .collect(Collectors.groupingBy(result -> result.getQuiz().getId()));

        return quizRepository.findByCourse_Id(courseId).stream()
                .map(quiz -> new ChartPoint(quiz.getName(),
                        averagePercentage(resultsByQuiz.getOrDefault(quiz.getId(), List.of()))))
                .toList();
    }

    private static double averagePercentage(List<QuizResult> results) {
        int totalScore = results.stream().mapToInt(QuizResult::getScore).sum();
        int totalQuestions = results.stream().mapToInt(QuizResult::getTotalQuestions).sum();
        return totalQuestions == 0 ? 0d : (double) totalScore / totalQuestions * 100.0;
    }
}
