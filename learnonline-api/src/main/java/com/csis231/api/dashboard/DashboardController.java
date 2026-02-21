package com.csis231.api.dashboard;

import com.csis231.api.common.ResourceNotFoundException;
import com.csis231.api.common.UnauthorizedException;
import com.csis231.api.course.Course;
import com.csis231.api.course.CourseDto;
import com.csis231.api.course.CourseMapper;
import com.csis231.api.course.CourseService;
import com.csis231.api.enrollment.CourseEnrollment;
import com.csis231.api.enrollment.EnrollmentService;
import com.csis231.api.quiz.*;
import com.csis231.api.user.User;
import com.csis231.api.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Dashboards for students and instructors.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DashboardController {

    private final UserRepository userRepository;
    private final CourseService courseService;
    private final EnrollmentService enrollmentService;
    private final QuizService quizService;
    private final QuizRepository quizRepository;
    private final QuizQuestionRepository questionRepository;

    /**
     * Builds the student dashboard with enrollments, recent quiz results, and upcoming quizzes.
     *
     * @param authentication the authenticated principal
     * @return a {@link StudentDashboardResponse} containing dashboard data
     */
    @GetMapping("/student/dashboard")
    public StudentDashboardResponse studentDashboard(Authentication authentication) {
        User student = resolveUser(authentication);
        if (student.getRole() != User.Role.STUDENT && student.getRole() != User.Role.ADMIN) {
            throw new UnauthorizedException("Only students can access the student dashboard");
        }
        List<CourseEnrollment> enrollments = enrollmentService.findByStudent(student.getId());
        List<CourseDto> enrolledCourses = enrollments.stream()
                .map(CourseEnrollment::getCourse)
                .map(CourseMapper::toDto)
                .toList();

        List<QuizResultDto> recentResults = quizService.latestResultsForStudent(student.getId());

        List<QuizSummaryDto> quizzes = enrollments.stream()
                .flatMap(e -> quizRepository.findByCourse_Id(e.getCourse().getId()).stream())
                .map(q -> QuizMapper.toSummaryDto(q, questionRepository.findByQuiz_Id(q.getId()).size()))
                .collect(Collectors.toList());

        return new StudentDashboardResponse(
                student.getId(),
                enrolledCourses.size(),
                enrolledCourses,
                recentResults,
                quizzes
        );
    }

    /**
     * Builds the instructor dashboard with owned courses and aggregated stats.
     *
     * @param authentication the authenticated principal
     * @return an {@link InstructorDashboardResponse} summarizing instructor metrics
     */
    @GetMapping("/instructor/dashboard")
    public InstructorDashboardResponse instructorDashboard(Authentication authentication) {
        User instructor = resolveUser(authentication);
        if (instructor.getRole() != User.Role.INSTRUCTOR && instructor.getRole() != User.Role.ADMIN) {
            throw new UnauthorizedException("Only instructors can access the instructor dashboard");
        }
        List<Course> courses = courseService.listByInstructor(instructor.getId());
        List<CourseDto> courseDtos = courses.stream().map(CourseMapper::toDto).toList();

        List<CourseStatsDto> stats = courses.stream()
                .map(c -> new CourseStatsDto(
                        c.getId(),
                        c.getTitle(),
                        enrollmentService.countForCourse(c.getId()),
                        quizRepository.findByCourse_Id(c.getId()).size()
                ))
                .toList();

        long totalEnrollments = stats.stream().mapToLong(CourseStatsDto::enrollmentCount).sum();

        return new InstructorDashboardResponse(
                instructor.getId(),
                courses.size(),
                totalEnrollments,
                courseDtos,
                stats
        );
    }

    /**
     * Lists courses belonging to the specified instructor (or all, if admin).
     *
     * @param userId         the instructor's user id
     * @param authentication the authenticated principal
     * @return a list of {@link CourseDto} owned by the instructor
     */
    @GetMapping("/instructors/{userId}/courses")
    public List<CourseDto> coursesByInstructor(@PathVariable Long userId, Authentication authentication) {
        User actor = resolveUser(authentication);
        if (!actor.getId().equals(userId) && actor.getRole() != User.Role.ADMIN) {
            throw new UnauthorizedException("You cannot view courses for this instructor");
        }
        return courseService.listByInstructor(userId).stream()
                .map(CourseMapper::toDto)
                .toList();
    }

    private User resolveUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new UnauthorizedException("Authentication required");
        }
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + authentication.getName()));
    }
}
