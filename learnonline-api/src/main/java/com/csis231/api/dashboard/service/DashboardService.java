package com.csis231.api.dashboard.service;

import com.csis231.api.common.exception.UnauthorizedException;
import com.csis231.api.course.dto.CourseDto;
import com.csis231.api.course.mapper.CourseMapper;
import com.csis231.api.course.model.Course;
import com.csis231.api.course.service.CourseService;
import com.csis231.api.dashboard.dto.CourseStatsDto;
import com.csis231.api.dashboard.dto.InstructorDashboardResponse;
import com.csis231.api.dashboard.dto.StudentDashboardResponse;
import com.csis231.api.enrollment.model.CourseEnrollment;
import com.csis231.api.enrollment.service.EnrollmentService;
import com.csis231.api.quiz.dto.QuizResultDto;
import com.csis231.api.quiz.dto.QuizSummaryDto;
import com.csis231.api.quiz.mapper.QuizMapper;
import com.csis231.api.quiz.repository.QuizQuestionRepository;
import com.csis231.api.quiz.repository.QuizRepository;
import com.csis231.api.quiz.service.QuizService;
import com.csis231.api.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final CourseService courseService;
    private final EnrollmentService enrollmentService;
    private final QuizService quizService;
    private final QuizRepository quizRepository;
    private final QuizQuestionRepository questionRepository;

    @Transactional(readOnly = true)
    public StudentDashboardResponse buildStudentDashboard(User student) {
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
                .toList();

        return new StudentDashboardResponse(
                student.getId(),
                enrolledCourses.size(),
                enrolledCourses,
                recentResults,
                quizzes
        );
    }

    @Transactional(readOnly = true)
    public InstructorDashboardResponse buildInstructorDashboard(User instructor) {
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

    @Transactional(readOnly = true)
    public List<CourseDto> listCoursesByInstructor(User actor, Long userId) {
        if (!actor.getId().equals(userId) && actor.getRole() != User.Role.ADMIN) {
            throw new UnauthorizedException("You cannot view courses for this instructor");
        }
        return courseService.listByInstructor(userId).stream()
                .map(CourseMapper::toDto)
                .toList();
    }
}
