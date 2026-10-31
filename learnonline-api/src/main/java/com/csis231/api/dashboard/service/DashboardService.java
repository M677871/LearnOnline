package com.csis231.api.dashboard.service;

import com.csis231.api.common.exception.ForbiddenException;
import com.csis231.api.course.dto.CourseDto;
import com.csis231.api.course.service.CourseService;
import com.csis231.api.dashboard.dto.CourseStatsDto;
import com.csis231.api.dashboard.dto.InstructorDashboardResponse;
import com.csis231.api.dashboard.dto.StudentDashboardResponse;
import com.csis231.api.enrollment.service.EnrollmentService;
import com.csis231.api.quiz.dto.QuizSummaryDto;
import com.csis231.api.quiz.service.QuizService;
import com.csis231.api.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Composes read-only dashboards from the services of the other features.
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final CourseService courseService;
    private final EnrollmentService enrollmentService;
    private final QuizService quizService;

    @Transactional(readOnly = true)
    public StudentDashboardResponse buildStudentDashboard(User student) {
        if (!student.isStudent() && !student.isAdmin()) {
            throw new ForbiddenException("Only students can access the student dashboard");
        }

        List<CourseDto> enrolledCourses = enrollmentService.enrolledCourses(student.getId());
        List<QuizSummaryDto> quizzes = enrolledCourses.stream()
                .flatMap(course -> quizService.summariesForCourse(course.id()).stream())
                .toList();

        return new StudentDashboardResponse(
                student.getId(),
                enrolledCourses.size(),
                enrolledCourses,
                quizService.latestResultsForStudent(student.getId()),
                quizzes
        );
    }

    @Transactional(readOnly = true)
    public InstructorDashboardResponse buildInstructorDashboard(User instructor) {
        if (!instructor.isInstructor() && !instructor.isAdmin()) {
            throw new ForbiddenException("Only instructors can access the instructor dashboard");
        }

        List<CourseDto> courses = courseService.listByInstructor(instructor.getId());
        List<CourseStatsDto> stats = courses.stream()
                .map(course -> new CourseStatsDto(
                        course.id(),
                        course.title(),
                        enrollmentService.countForCourse(course.id()),
                        quizService.countForCourse(course.id())
                ))
                .toList();
        long totalEnrollments = stats.stream().mapToLong(CourseStatsDto::enrollmentCount).sum();

        return new InstructorDashboardResponse(
                instructor.getId(),
                courses.size(),
                totalEnrollments,
                courses,
                stats
        );
    }

    @Transactional(readOnly = true)
    public List<CourseDto> listCoursesByInstructor(User actor, Long userId) {
        if (!actor.getId().equals(userId) && !actor.isAdmin()) {
            throw new ForbiddenException("You cannot view courses for this instructor");
        }
        return courseService.listByInstructor(userId);
    }
}
