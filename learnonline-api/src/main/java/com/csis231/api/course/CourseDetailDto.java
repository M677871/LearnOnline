package com.csis231.api.course;

import com.csis231.api.coursematerial.CourseMaterialDto;
import com.csis231.api.quiz.QuizSummaryDto;

import java.time.Instant;
import java.util.List;

/**
 * Detailed course DTO including related materials and quizzes.
 */
public record CourseDetailDto(
        Long id,
        String title,
        String description,
        Long instructorUserId,
        String instructorName,
        Long categoryId,
        Boolean published,
        Instant createdAt,
        Instant updatedAt,
        List<CourseMaterialDto> materials,
        List<QuizSummaryDto> quizzes
) {}
