package com.csis231.api.dashboard.controller;

import com.csis231.api.course.dto.CourseDto;
import com.csis231.api.dashboard.dto.InstructorDashboardResponse;
import com.csis231.api.dashboard.dto.StudentDashboardResponse;
import com.csis231.api.dashboard.service.DashboardService;
import com.csis231.api.user.service.AuthenticatedUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DashboardController {

    private final AuthenticatedUserService authenticatedUserService;
    private final DashboardService dashboardService;

    @GetMapping("/student/dashboard")
    public StudentDashboardResponse studentDashboard(Authentication authentication) {
        return dashboardService.buildStudentDashboard(authenticatedUserService.require(authentication));
    }

    @GetMapping("/instructor/dashboard")
    public InstructorDashboardResponse instructorDashboard(Authentication authentication) {
        return dashboardService.buildInstructorDashboard(authenticatedUserService.require(authentication));
    }

    @GetMapping("/instructors/{userId}/courses")
    public List<CourseDto> coursesByInstructor(@PathVariable Long userId, Authentication authentication) {
        return dashboardService.listCoursesByInstructor(authenticatedUserService.require(authentication), userId);
    }
}
