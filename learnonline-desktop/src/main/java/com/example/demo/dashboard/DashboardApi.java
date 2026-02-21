package com.example.demo.dashboard;

import com.example.demo.common.ApiClient;
import com.example.demo.common.ApiResponse;
import com.example.demo.model.InstructorDashboardResponse;
import com.example.demo.model.StudentDashboardResponse;
import com.fasterxml.jackson.core.type.TypeReference;

/**
 * Wrapper for dashboard endpoints.
 */
public class DashboardApi {
    private final ApiClient client = new ApiClient();

    /**
     * Fetches the student dashboard summary for the current authenticated user.
     *
     * @return recent quiz results and related stats
     */
    public StudentDashboardResponse studentDashboard() {
        ApiResponse<StudentDashboardResponse> resp = client.get("/api/student/dashboard", new TypeReference<StudentDashboardResponse>() {});
        return resp.getBody();
    }

    /**
     * Fetches the instructor dashboard summary for the current authenticated user.
     *
     * @return course counts and enrollment stats
     */
    public InstructorDashboardResponse instructorDashboard() {
        ApiResponse<InstructorDashboardResponse> resp = client.get("/api/instructor/dashboard", new TypeReference<InstructorDashboardResponse>() {});
        return resp.getBody();
    }
}
