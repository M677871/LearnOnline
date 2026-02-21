package com.csis231.api.dashboard;

import com.csis231.api.course.CourseDto;
import com.csis231.api.quiz.QuizResultDto;
import com.csis231.api.quiz.QuizSummaryDto;

import java.util.List;

/**
 * DTO summarizing student-facing dashboard data.
 */
public record StudentDashboardResponse(
        Long studentUserId,
        int enrolledCourseCount,
        List<CourseDto> enrolledCourses,
        List<QuizResultDto> recentQuizResults,
        List<QuizSummaryDto> upcomingQuizzes
) {}
