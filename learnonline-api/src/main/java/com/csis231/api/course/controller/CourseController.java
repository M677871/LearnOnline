package com.csis231.api.course.controller;

import com.csis231.api.common.dto.PagedResponse;
import com.csis231.api.common.service.AuthenticatedUserService;
import com.csis231.api.course.dto.CourseDetailDto;
import com.csis231.api.course.dto.CourseDto;
import com.csis231.api.course.dto.CourseRequest;
import com.csis231.api.course.mapper.CourseMapper;
import com.csis231.api.course.model.Course;
import com.csis231.api.course.service.CourseService;
import com.csis231.api.user.model.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller exposing course catalog and management endpoints.
 */
@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;
    private final AuthenticatedUserService authenticatedUserService;

    /**
     * Retrieves a paginated list of published courses, optionally filtered by category or search term.
     *
     * @param page       the zero-based page index to fetch
     * @param size       the number of items per page
     * @param categoryId optional category filter
     * @param search     optional search text applied to title/description
     * @return a {@link PagedResponse} containing course summaries and page metadata
     */
    @GetMapping
    public PagedResponse<CourseDto> list(@RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "10") int size,
                                         @RequestParam(required = false) Long categoryId,
                                         @RequestParam(required = false) String search) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));
        var springPage = courseService.listPublished(categoryId, search, pageable)
                .map(CourseMapper::toDto);
        return PagedResponse.fromPage(springPage);
    }

    /**
     * Retrieves full course details for a given course identifier.
     *
     * @param id             the course ID to fetch
     * @param authentication the current authenticated principal (may be null)
     * @return a {@link CourseDetailDto} with materials and quizzes visible to the viewer
     */
    @GetMapping("/{id}")
    public CourseDetailDto detail(@PathVariable Long id, Authentication authentication) {
        User viewer = authenticatedUserService.optional(authentication);
        return courseService.getCourseDetail(id, viewer);
    }

    /**
     * Creates a new course on behalf of the authenticated instructor/admin.
     *
     * @param request        the course creation payload
     * @param authentication the authenticated principal creating the course
     * @return {@code 201 Created} with the created {@link CourseDto}
     */
    @PostMapping
    public ResponseEntity<CourseDto> create(@Valid @RequestBody CourseRequest request,
                                            Authentication authentication) {
        User actor = authenticatedUserService.require(authentication);
        Course created = courseService.createCourse(request, actor);
        return ResponseEntity.status(201).body(CourseMapper.toDto(created));
    }

    /**
     * Updates an existing course. Only instructors/admins (or the owner) may modify it.
     *
     * @param id             the course ID to update
     * @param request        the update payload
     * @param authentication the authenticated principal performing the update
     * @return the updated {@link CourseDto}
     */
    @PutMapping("/{id}")
    public CourseDto update(@PathVariable Long id,
                            @Valid @RequestBody CourseRequest request,
                            Authentication authentication) {
        User actor = authenticatedUserService.require(authentication);
        Course updated = courseService.updateCourse(id, request, actor);
        return CourseMapper.toDto(updated);
    }

    /**
     * Deletes a course. Only the owning instructor or an admin may delete.
     *
     * @param id             the course ID to delete
     * @param authentication the authenticated principal performing the deletion
     * @return {@link ResponseEntity} with no content on success
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication authentication) {
        User actor = authenticatedUserService.require(authentication);
        courseService.deleteCourse(id, actor);
        return ResponseEntity.noContent().build();
    }
}
