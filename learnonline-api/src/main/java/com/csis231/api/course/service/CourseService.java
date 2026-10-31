package com.csis231.api.course.service;

import com.csis231.api.category.repository.CategoryRepository;
import com.csis231.api.common.dto.PagedResponse;
import com.csis231.api.course.dto.CourseDetailDto;
import com.csis231.api.course.dto.CourseDto;
import com.csis231.api.course.dto.CourseRequest;
import com.csis231.api.course.mapper.CourseMapper;
import com.csis231.api.course.model.Course;
import com.csis231.api.course.repository.CourseRepository;
import com.csis231.api.coursematerial.service.CourseMaterialService;
import com.csis231.api.quiz.service.QuizService;
import com.csis231.api.user.model.User;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CourseService {

    private static final String NOT_STAFF = "Only instructors or admins can modify courses";
    private static final String NOT_OWNER = "You cannot modify courses you do not own";

    private final CourseRepository courseRepository;
    private final CategoryRepository categoryRepository;
    private final CourseMaterialService materialService;
    private final QuizService quizService;

    @Transactional
    public CourseDto createCourse(CourseRequest req, User actor) {
        CourseAccess.requireStaff(actor, NOT_STAFF);

        Course course = Course.builder()
                .title(req.title())
                .description(req.description())
                .instructor(actor)
                .category(req.categoryId() != null ? categoryRepository.getOrThrow(req.categoryId()) : null)
                .published(Boolean.TRUE.equals(req.published()))
                .build();
        return CourseMapper.toDto(courseRepository.save(course));
    }

    /**
     * Applies the non-null fields of the request. Flushes before mapping because
     * {@code updatedAt} is only assigned when Hibernate writes the row.
     */
    @Transactional
    public CourseDto updateCourse(Long id, CourseRequest req, User actor) {
        Course course = courseRepository.getOrThrow(id);
        CourseAccess.requireManager(course, actor, NOT_STAFF, NOT_OWNER);

        if (req.title() != null) {
            course.setTitle(req.title());
        }
        if (req.description() != null) {
            course.setDescription(req.description());
        }
        if (req.published() != null) {
            course.setPublished(req.published());
        }
        if (req.categoryId() != null) {
            course.setCategory(categoryRepository.getOrThrow(req.categoryId()));
        }
        return CourseMapper.toDto(courseRepository.saveAndFlush(course));
    }

    @Transactional
    public void deleteCourse(Long id, User actor) {
        Course course = courseRepository.getOrThrow(id);
        CourseAccess.requireManager(course, actor, NOT_STAFF, NOT_OWNER);
        courseRepository.delete(course);
    }

    @Transactional(readOnly = true)
    public PagedResponse<CourseDto> listPublished(Long categoryId, String search, Pageable pageable) {
        Specification<Course> spec = isPublished();
        if (categoryId != null) {
            spec = spec.and(hasCategory(categoryId));
        }
        if (search != null && !search.isBlank()) {
            spec = spec.and(matchesSearch(search));
        }
        return PagedResponse.fromPage(courseRepository.findAll(spec, pageable).map(CourseMapper::toDto));
    }

    @Transactional(readOnly = true)
    public List<CourseDto> listByInstructor(Long instructorId) {
        return courseRepository.findByInstructor_Id(instructorId).stream()
                .map(CourseMapper::toDto)
                .toList();
    }

    /**
     * Builds the course page for a viewer, who is {@code null} when anonymous.
     * The material rules of {@link CourseMaterialService#listForViewer} apply to the whole page.
     */
    @Transactional(readOnly = true)
    public CourseDetailDto getCourseDetail(Long id, User viewer) {
        return CourseMapper.toDetailDto(
                courseRepository.getOrThrow(id),
                materialService.listForViewer(id, viewer),
                quizService.summariesForCourse(id)
        );
    }

    private static Specification<Course> isPublished() {
        return (root, query, cb) -> cb.isTrue(root.get("published"));
    }

    private static Specification<Course> hasCategory(Long categoryId) {
        return (root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId);
    }

    private static Specification<Course> matchesSearch(String search) {
        return (root, query, cb) -> {
            String like = "%" + search.toLowerCase() + "%";
            Predicate title = cb.like(cb.lower(root.get("title")), like);
            Predicate description = cb.like(cb.lower(root.get("description")), like);
            return cb.or(title, description);
        };
    }
}
