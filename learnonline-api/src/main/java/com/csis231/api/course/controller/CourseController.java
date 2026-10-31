package com.csis231.api.course.controller;

import com.csis231.api.common.dto.PagedResponse;
import com.csis231.api.course.dto.CourseDetailDto;
import com.csis231.api.course.dto.CourseDto;
import com.csis231.api.course.dto.CourseRequest;
import com.csis231.api.course.service.CourseService;
import com.csis231.api.user.service.AuthenticatedUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;
    private final AuthenticatedUserService authenticatedUserService;

    /**
     * Public catalog of published courses. Unlike the admin lists, invalid paging values are
     * corrected rather than rejected.
     */
    @GetMapping
    public PagedResponse<CourseDto> list(@RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "10") int size,
                                         @RequestParam(required = false) Long categoryId,
                                         @RequestParam(required = false) String search) {
        return courseService.listPublished(categoryId, search, PageRequest.of(Math.max(0, page), Math.max(1, size)));
    }

    @GetMapping("/{id}")
    public CourseDetailDto detail(@PathVariable Long id, Authentication authentication) {
        return courseService.getCourseDetail(id, authenticatedUserService.optional(authentication));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CourseDto create(@Valid @RequestBody CourseRequest request, Authentication authentication) {
        return courseService.createCourse(request, authenticatedUserService.require(authentication));
    }

    @PutMapping("/{id}")
    public CourseDto update(@PathVariable Long id,
                            @Valid @RequestBody CourseRequest request,
                            Authentication authentication) {
        return courseService.updateCourse(id, request, authenticatedUserService.require(authentication));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, Authentication authentication) {
        courseService.deleteCourse(id, authenticatedUserService.require(authentication));
    }
}
