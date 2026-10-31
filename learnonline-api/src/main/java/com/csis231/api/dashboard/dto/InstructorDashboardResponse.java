package com.csis231.api.dashboard.dto;

import com.csis231.api.course.dto.CourseDto;

import java.util.List;

public record InstructorDashboardResponse(
        Long instructorUserId,
        int courseCount,
        long totalEnrollments,
        List<CourseDto> courses,
        List<CourseStatsDto> courseStats
) {
}
