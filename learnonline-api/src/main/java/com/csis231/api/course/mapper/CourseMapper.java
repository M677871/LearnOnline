package com.csis231.api.course.mapper;

import com.csis231.api.course.dto.CourseDetailDto;
import com.csis231.api.course.dto.CourseDto;
import com.csis231.api.course.model.Course;
import com.csis231.api.coursematerial.dto.CourseMaterialDto;
import com.csis231.api.quiz.dto.QuizSummaryDto;

import java.util.List;

public final class CourseMapper {

    private CourseMapper() {
    }

    public static CourseDto toDto(Course course) {
        return new CourseDto(
                course.getId(),
                course.getTitle(),
                course.getDescription(),
                course.getInstructor().getId(),
                course.getInstructor().getUsername(),
                course.getCategory() != null ? course.getCategory().getId() : null,
                course.getPublished(),
                course.getCreatedAt(),
                course.getUpdatedAt()
        );
    }

    public static CourseDetailDto toDetailDto(Course course,
                                              List<CourseMaterialDto> materials,
                                              List<QuizSummaryDto> quizzes) {
        CourseDto base = toDto(course);
        return new CourseDetailDto(
                base.id(),
                base.title(),
                base.description(),
                base.instructorUserId(),
                base.instructorName(),
                base.categoryId(),
                base.published(),
                base.createdAt(),
                base.updatedAt(),
                materials,
                quizzes
        );
    }
}
