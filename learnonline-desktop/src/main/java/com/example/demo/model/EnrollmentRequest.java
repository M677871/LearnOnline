package com.example.demo.model;

/**
 * Request body for enrolling a student in a course.
 *
 * <p>Used by enrollment flows where either an admin/instructor specifies a
 * student id or the student enrolls themselves.</p>
 */
public class EnrollmentRequest {
    private Long studentUserId;
    private Long courseId;

    public EnrollmentRequest() {}
    public EnrollmentRequest(Long studentUserId, Long courseId) {
        this.studentUserId = studentUserId;
        this.courseId = courseId;
    }

    public Long getStudentUserId() { return studentUserId; }
    public void setStudentUserId(Long studentUserId) { this.studentUserId = studentUserId; }

    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }
}
