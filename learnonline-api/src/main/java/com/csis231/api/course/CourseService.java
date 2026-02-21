package com.csis231.api.course;

import com.csis231.api.category.Category;
import com.csis231.api.category.CategoryRepository;
import com.csis231.api.common.BadRequestException;
import com.csis231.api.common.ResourceNotFoundException;
import com.csis231.api.common.UnauthorizedException;
import com.csis231.api.coursematerial.CourseMaterialDto;
import com.csis231.api.coursematerial.CourseMaterialMapper;
import com.csis231.api.coursematerial.CourseMaterialRepository;
import com.csis231.api.coursematerial.CourseMaterialService;
import com.csis231.api.quiz.QuizRepository;
import com.csis231.api.quiz.QuizSummaryDto;
import com.csis231.api.quiz.QuizMapper;
import com.csis231.api.quiz.QuizQuestionRepository;
import com.csis231.api.user.User;
import com.csis231.api.user.UserRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Application service encapsulating course operations and authorization checks.
 */
@Service
@RequiredArgsConstructor
public class CourseService {
    private final CourseRepository courseRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final CourseMaterialRepository materialRepository;
    private final CourseMaterialService materialService;
    private final QuizRepository quizRepository;
    private final QuizQuestionRepository questionRepository;

    /**
     * Creates a new course for the given instructor/admin actor.
     *
     * @param req   the course creation request payload
     * @param actor the authenticated user performing the operation
     * @return the persisted {@link Course}
     * @throws BadRequestException      if the payload is missing
     * @throws UnauthorizedException    if the actor is not instructor/admin
     * @throws ResourceNotFoundException if the category is not found
     */
    @Transactional
    public Course createCourse(CourseRequest req, User actor) {
        if (req == null) throw new BadRequestException("Course payload is required");
        if (actor == null) throw new UnauthorizedException("Authenticated user required");
        requireInstructorOrAdmin(actor);

        Category category = null;
        if (req.categoryId() != null) {
            category = categoryRepository.findById(req.categoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + req.categoryId()));
        }

        Course course = Course.builder()
                .title(req.title())
                .description(req.description())
                .instructor(actor)
                .category(category)
                .published(Boolean.TRUE.equals(req.published()))
                .build();
        return courseRepository.save(course);
    }

    /**
     * Updates an existing course with the supplied fields.
     *
     * @param id    the course identifier
     * @param req   the update payload
     * @param actor the authenticated user performing the update
     * @return the updated {@link Course}
     * @throws BadRequestException      if payload is missing
     * @throws UnauthorizedException    if actor cannot modify the course
     * @throws ResourceNotFoundException if course or category are not found
     */
    @Transactional
    public Course updateCourse(Long id, CourseRequest req, User actor) {
        if (req == null) throw new BadRequestException("Course payload is required");
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found: " + id));
        if (actor == null) throw new UnauthorizedException("Authenticated user required");
        requireOwnerOrAdmin(course, actor);

        if (req.title() != null) course.setTitle(req.title());
        if (req.description() != null) course.setDescription(req.description());
        if (req.published() != null) course.setPublished(req.published());

        if (req.categoryId() != null) {
            Category category = categoryRepository.findById(req.categoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + req.categoryId()));
            course.setCategory(category);
        }

        return courseRepository.save(course);
    }

    /**
     * Retrieves a course by id or throws if missing.
     *
     * @param id the course identifier
     * @return the matching {@link Course}
     * @throws ResourceNotFoundException if no course exists with this id
     */
    @Transactional(readOnly = true)
    public Course getCourseOrThrow(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found: " + id));
    }

    /**
     * Deletes a course after verifying ownership or admin privileges.
     *
     * @param id    the course identifier
     * @param actor the authenticated user performing the deletion
     * @throws UnauthorizedException    if actor is not allowed to delete
     * @throws ResourceNotFoundException if the course does not exist
     */
    @Transactional
    public void deleteCourse(Long id, User actor) {
        Course course = getCourseOrThrow(id);
        if (actor == null) throw new UnauthorizedException("Authenticated user required");
        requireOwnerOrAdmin(course, actor);
        courseRepository.delete(course);
    }

    /**
     * Lists published courses with optional category and search filters.
     *
     * @param categoryId optional category filter
     * @param search     optional search text
     * @param pageable   pagination information
     * @return a page of {@link Course} entities
     */
    @Transactional(readOnly = true)
    public Page<Course> listPublished(Long categoryId, String search, Pageable pageable) {
        Specification<Course> spec = Specification.where(isPublished());
        if (categoryId != null) {
            spec = spec.and(hasCategory(categoryId));
        }
        if (search != null && !search.isBlank()) {
            spec = spec.and(matchesSearch(search));
        }
        return courseRepository.findAll(spec, pageable);
    }

    /**
     * Retrieves courses taught by a specific instructor using pagination.
     *
     * @param instructorId the instructor user id
     * @param pageable     pagination information
     * @return a page of courses
     */
    @Transactional(readOnly = true)
    public Page<Course> listByInstructor(Long instructorId, Pageable pageable) {
        return courseRepository.findByInstructor_Id(instructorId, pageable);
    }

    /**
     * Retrieves all courses taught by a specific instructor.
     *
     * @param instructorId the instructor user id
     * @return list of courses
     */
    @Transactional(readOnly = true)
    public List<Course> listByInstructor(Long instructorId) {
        return courseRepository.findByInstructor_Id(instructorId);
    }

    /**
     * Builds a detailed course view including materials and quizzes for a given viewer.
     *
     * @param id     the course id
     * @param viewer the user requesting the data (may be null)
     * @return a {@link CourseDetailDto} with materials/quizzes the viewer can see
     */
    @Transactional(readOnly = true)
    public CourseDetailDto getCourseDetail(Long id, User viewer) {
        Course course = getCourseOrThrow(id);
        List<CourseMaterialDto> materials = materialService.listForViewer(id, viewer).stream()
                .map(CourseMaterialMapper::toDto)
                .collect(Collectors.toList());
        List<QuizSummaryDto> quizzes = quizRepository.findByCourse_Id(id).stream()
                .map(q -> {
                    int count = questionRepository.findByQuiz_Id(q.getId()).size();
                    return QuizMapper.toSummaryDto(q, count);
                })
                .collect(Collectors.toList());
        return CourseMapper.toDetailDto(course, materials, quizzes);
    }

    private static void requireInstructorOrAdmin(User actor) {
        if (actor.getRole() == null) throw new UnauthorizedException("Role required");
        switch (actor.getRole()) {
            case ADMIN, INSTRUCTOR -> {}
            default -> throw new UnauthorizedException("Only instructors or admins can modify courses");
        }
    }

    private static void requireOwnerOrAdmin(Course course, User actor) {
        Objects.requireNonNull(course, "course");
        requireInstructorOrAdmin(actor);
        boolean isOwner = course.getInstructor() != null && course.getInstructor().getId() != null
                && course.getInstructor().getId().equals(actor.getId());
        if (!isOwner && actor.getRole() != User.Role.ADMIN) {
            throw new UnauthorizedException("You cannot modify courses you do not own");
        }
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
            Predicate desc = cb.like(cb.lower(root.get("description")), like);
            return cb.or(title, desc);
        };
    }

    @Transactional(readOnly = true)
    public Optional<User> findUser(Long id) {
        return userRepository.findById(id);
    }
}
