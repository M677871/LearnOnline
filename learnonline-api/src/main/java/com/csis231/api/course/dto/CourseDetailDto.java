package com.csis231.api.course.dto;

import com.csis231.api.coursematerial.dto.CourseMaterialDto;
import com.csis231.api.quiz.dto.QuizSummaryDto;

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
