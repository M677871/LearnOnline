package com.csis231.api.dashboard.controller;

import com.csis231.api.common.service.AuthenticatedUserService;
import com.csis231.api.course.dto.CourseDto;
import com.csis231.api.dashboard.dto.InstructorDashboardResponse;
import com.csis231.api.dashboard.dto.StudentDashboardResponse;
import com.csis231.api.dashboard.service.DashboardService;
import com.csis231.api.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Dashboards for students and instructors.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DashboardController {

    private final AuthenticatedUserService authenticatedUserService;
    private final DashboardService dashboardService;

    /**
     * Builds the student dashboard with enrollments, recent quiz results, and upcoming quizzes.
     *
     * @param authentication the authenticated principal
     * @return a {@link StudentDashboardResponse} containing dashboard data
     */
    @GetMapping("/student/dashboard")
    public StudentDashboardResponse studentDashboard(Authentication authentication) {
        User student = authenticatedUserService.require(authentication);
        return dashboardService.buildStudentDashboard(student);
    }

    /**
     * Builds the instructor dashboard with owned courses and aggregated stats.
     *
     * @param authentication the authenticated principal
     * @return an {@link InstructorDashboardResponse} summarizing instructor metrics
     */
    @GetMapping("/instructor/dashboard")
    public InstructorDashboardResponse instructorDashboard(Authentication authentication) {
        User instructor = authenticatedUserService.require(authentication);
        return dashboardService.buildInstructorDashboard(instructor);
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
        User actor = authenticatedUserService.require(authentication);
        return dashboardService.listCoursesByInstructor(actor, userId);
    }
}
