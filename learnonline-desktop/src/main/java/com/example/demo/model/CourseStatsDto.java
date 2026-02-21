package com.example.demo.model;

/**
 * Aggregated metrics for an instructor course.
 *
 * <p>Returned by instructor dashboard/analytics to show course-level enrollments
 * and quiz counts.</p>
 */
public class CourseStatsDto {
    private Long courseId;
    private String courseTitle;
    private long enrollmentCount;
    private int quizCount;

    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }

    public String getCourseTitle() { return courseTitle; }
    public void setCourseTitle(String courseTitle) { this.courseTitle = courseTitle; }

    public long getEnrollmentCount() { return enrollmentCount; }
    public void setEnrollmentCount(long enrollmentCount) { this.enrollmentCount = enrollmentCount; }

    public int getQuizCount() { return quizCount; }
    public void setQuizCount(int quizCount) { this.quizCount = quizCount; }
}
