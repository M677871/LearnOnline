package com.csis231.api.dashboard;

import com.csis231.api.common.UnauthorizedException;
import com.csis231.api.common.ResourceNotFoundException;
import com.csis231.api.course.Course;
import com.csis231.api.course.CourseRepository;
import com.csis231.api.quiz.Quiz;
import com.csis231.api.quiz.QuizRepository;
import com.csis231.api.quiz.QuizResult;
import com.csis231.api.quiz.QuizResultRepository;
import com.csis231.api.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Read-only statistics aggregation for visualizations (2D/3D).
 */
@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final CourseRepository courseRepository;
    private final QuizRepository quizRepository;
    private final QuizResultRepository quizResultRepository;

    /**
     * Returns quiz average scores (percentage) for every quiz in the given course.
     * Access: admin, or the instructor who owns the course.
     *
     * @param courseId course identifier
     * @param actor    authenticated user requesting the data
     * @return list of chart points (quiz name + average score)
     */
    @Transactional(readOnly = true)
    public List<ChartPoint> quizAveragesForCourse(Long courseId, User actor) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found: " + courseId));
        ensureAdminOrOwner(actor, course);

        List<ChartPoint> points = new ArrayList<>();
        List<Quiz> quizzes = quizRepository.findByCourse_Id(courseId);
        for (Quiz quiz : quizzes) {
            List<QuizResult> results = quizResultRepository.findByQuiz_Id(quiz.getId());
            if (results.isEmpty()) {
                points.add(new ChartPoint(quiz.getName(), 0d));
                continue;
            }
            int totalScore = results.stream().mapToInt(QuizResult::getScore).sum();
            int totalQuestions = results.stream().mapToInt(QuizResult::getTotalQuestions).sum();
            double avg = totalQuestions == 0 ? 0d : (double) totalScore / totalQuestions * 100.0;
            points.add(new ChartPoint(quiz.getName(), avg));
        }
        return points;
    }

    private void ensureAdminOrOwner(User actor, Course course) {
        if (actor == null) {
            throw new UnauthorizedException("Authentication required");
        }
        if (actor.getRole() == User.Role.ADMIN) {
            return;
        }
        if (actor.getRole() == User.Role.INSTRUCTOR
                && course.getInstructor() != null
                && Objects.equals(course.getInstructor().getId(), actor.getId())) {
            return;
        }
        throw new UnauthorizedException("Not authorized to view course statistics");
    }
}
