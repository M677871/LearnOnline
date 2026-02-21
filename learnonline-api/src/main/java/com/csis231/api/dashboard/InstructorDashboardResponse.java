package com.csis231.api.dashboard;

import com.csis231.api.course.CourseDto;

import java.util.List;

/**
 * DTO summarizing instructor-facing dashboard data.
 */
public record InstructorDashboardResponse(
        Long instructorUserId,
        int courseCount,
        long totalEnrollments,
        List<CourseDto> courses,
        List<CourseStatsDto> courseStats
) {}
