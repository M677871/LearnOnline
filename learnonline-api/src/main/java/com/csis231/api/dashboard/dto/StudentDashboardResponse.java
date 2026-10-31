package com.csis231.api.dashboard.dto;

import com.csis231.api.course.dto.CourseDto;
import com.csis231.api.quiz.dto.QuizResultDto;
import com.csis231.api.quiz.dto.QuizSummaryDto;

import java.util.List;

public record StudentDashboardResponse(
        Long studentUserId,
        int enrolledCourseCount,
        List<CourseDto> enrolledCourses,
        List<QuizResultDto> recentQuizResults,
        List<QuizSummaryDto> upcomingQuizzes
) {
}
