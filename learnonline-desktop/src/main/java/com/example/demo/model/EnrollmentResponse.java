package com.example.demo.model;

import java.time.Instant;

/**
 * Enrollment DTO returned by backend.
 *
 * <p>Includes student identity, course info, status, and enrollment timestamp
 * so dashboards can display enrollment lists.</p>
 */
public class EnrollmentResponse {
    private Long id;
    private Long studentUserId;
    private String studentUsername;
    private String studentEmail;
    private Long courseId;
    private String courseTitle;
    private String status;
    private Instant enrolledAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getStudentUserId() { return studentUserId; }
    public void setStudentUserId(Long studentUserId) { this.studentUserId = studentUserId; }

    public String getStudentUsername() { return studentUsername; }
    public void setStudentUsername(String studentUsername) { this.studentUsername = studentUsername; }

    public String getStudentEmail() { return studentEmail; }
    public void setStudentEmail(String studentEmail) { this.studentEmail = studentEmail; }

    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }

    public String getCourseTitle() { return courseTitle; }
    public void setCourseTitle(String courseTitle) { this.courseTitle = courseTitle; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getEnrolledAt() { return enrolledAt; }
    public void setEnrolledAt(Instant enrolledAt) { this.enrolledAt = enrolledAt; }
}
