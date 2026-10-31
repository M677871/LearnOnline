package com.csis231.api.dashboard.dto;

public record CourseStatsDto(
        Long courseId,
        String courseTitle,
        long enrollmentCount,
        int quizCount
) {
}
