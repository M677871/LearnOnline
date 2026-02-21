package com.example.demo.student;

import com.example.demo.common.ApiClient;
import com.example.demo.common.ApiResponse;
import com.example.demo.model.EnrollmentRequest;
import com.example.demo.model.EnrollmentResponse;
import com.fasterxml.jackson.core.type.TypeReference;

/**
 * Client for enrollment endpoints.
 *
 * <p>Supports enrolling and listing a user's enrollments (current user or
 * explicit student id).</p>
 */
public class EnrollmentApi {
    private final ApiClient client = new ApiClient();

    /**
     * Enrolls a student in a course.
     */
    public EnrollmentResponse enroll(EnrollmentRequest req) {
        ApiResponse<EnrollmentResponse> resp = client.post("/api/enrollments/enroll", req, new TypeReference<EnrollmentResponse>() {});
        return resp.getBody();
    }

    /**
     * Lists enrollments for the given student id.
     */
    public EnrollmentResponse[] listByStudent(Long userId) {
        ApiResponse<EnrollmentResponse[]> resp = client.get("/api/students/" + userId + "/enrollments", new TypeReference<EnrollmentResponse[]>() {});
        return resp.getBody();
    }

    /**
     * Alias for listing the current user's enrollments.
     */
    public EnrollmentResponse[] listByCurrentUser(Long userId) {
        return listByStudent(userId);
    }
}
