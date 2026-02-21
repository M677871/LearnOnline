package com.example.demo.model;

import java.util.List;

/**
 * Payload returned by /api/instructor/dashboard.
 *
 * <p>Summarizes instructor metrics (course count, total enrollments) and
 * includes lists used to populate course tables and analytics.</p>
 */
public class InstructorDashboardResponse {
    private Long instructorUserId;
    private int courseCount;
    private long totalEnrollments;
    private List<CourseDto> courses;
    private List<CourseStatsDto> courseStats;

    public Long getInstructorUserId() { return instructorUserId; }
    public void setInstructorUserId(Long instructorUserId) { this.instructorUserId = instructorUserId; }

    public int getCourseCount() { return courseCount; }
    public void setCourseCount(int courseCount) { this.courseCount = courseCount; }

    public long getTotalEnrollments() { return totalEnrollments; }
    public void setTotalEnrollments(long totalEnrollments) { this.totalEnrollments = totalEnrollments; }

    public List<CourseDto> getCourses() { return courses; }
    public void setCourses(List<CourseDto> courses) { this.courses = courses; }

    public List<CourseStatsDto> getCourseStats() { return courseStats; }
    public void setCourseStats(List<CourseStatsDto> courseStats) { this.courseStats = courseStats; }
}
