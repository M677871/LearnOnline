package com.csis231.api.dashboard;

/**
 * Aggregated metrics for an instructor's course.
 */
public record CourseStatsDto(
        Long courseId,
        String courseTitle,
        long enrollmentCount,
        int quizCount
) {}
