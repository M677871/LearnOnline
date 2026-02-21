package com.example.demo.course;

import com.example.demo.common.ApiClient;
import com.example.demo.common.ApiResponse;
import com.example.demo.common.PageResponse;
import com.example.demo.model.*;
import com.fasterxml.jackson.core.type.TypeReference;

/**
 * HTTP wrapper for course-related endpoints.
 */
public class CourseApi {
    private final ApiClient client = new ApiClient();

    /**
     * Lists published courses with optional filtering.
     *
     * @param page       zero-based page index (negative values coerced to 0)
     * @param size       page size (values < 1 coerced to 1)
     * @param categoryId optional category filter
     * @param search     optional search text
     * @return a page of published courses
     */
    public PageResponse<CourseDto> listPublished(int page, int size, Long categoryId, String search) {
        StringBuilder path = new StringBuilder("/api/courses?page=" + Math.max(page, 0) + "&size=" + Math.max(size, 1));
        if (categoryId != null) path.append("&categoryId=").append(categoryId);
        if (search != null && !search.isBlank()) path.append("&search=").append(search.trim());
        ApiResponse<PageResponse<CourseDto>> resp = client.get(path.toString(), new TypeReference<PageResponse<CourseDto>>() {});
        return resp.getBody();
    }

    /**
     * Fetches a course detail by id.
     */
    public CourseDetailDto get(Long id) {
        ApiResponse<CourseDetailDto> resp = client.get("/api/courses/" + id, new TypeReference<CourseDetailDto>() {});
        return resp.getBody();
    }

    /**
     * Creates a new course.
     */
    public CourseDto create(CourseRequest req) {
        ApiResponse<CourseDto> resp = client.post("/api/courses", req, new TypeReference<CourseDto>() {});
        return resp.getBody();
    }

    /**
     * Updates an existing course by id.
     */
    public CourseDto update(Long id, CourseRequest req) {
        ApiResponse<CourseDto> resp = client.put("/api/courses/" + id, req, new TypeReference<CourseDto>() {});
        return resp.getBody();
    }

    /**
     * Deletes a course by id.
     */
    public void delete(Long id) {
        client.delete("/api/courses/" + id);
    }

    /**
     * Adds a material to a course.
     */
    public CourseMaterialDto addMaterial(Long courseId, CourseMaterialRequest req) {
        ApiResponse<CourseMaterialDto> resp = client.post("/api/courses/" + courseId + "/materials", req, new TypeReference<CourseMaterialDto>() {});
        return resp.getBody();
    }

    /**
     * Deletes a material by id.
     */
    public void deleteMaterial(Long materialId) {
        client.delete("/api/materials/" + materialId);
    }

    /**
     * Fetches a course detail and viewer-specific info (currently same as {@link #get(Long)}).
     */
    public CourseDetailDto getWithViewer(Long id) {
        return get(id);
    }

    /**
     * Lists all courses owned by an instructor.
     */
    public CourseDto[] listInstructorCourses(Long userId) {
        ApiResponse<CourseDto[]> resp = client.get("/api/instructors/" + userId + "/courses", new TypeReference<CourseDto[]>() {});
        return resp.getBody();
    }

    /**
     * Lists enrollments for a course.
     */
    public EnrollmentResponse[] listCourseEnrollments(Long courseId) {
        ApiResponse<EnrollmentResponse[]> resp = client.get("/api/courses/" + courseId + "/enrollments", new TypeReference<EnrollmentResponse[]>() {});
        return resp.getBody();
    }
}
