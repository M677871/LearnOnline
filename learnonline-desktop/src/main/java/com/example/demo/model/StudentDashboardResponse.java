package com.example.demo.model;

import java.util.List;

/**
 * Payload returned by /api/student/dashboard.
 *
 * <p>Contains course enrollment counts, recent quiz results, and upcoming
 * quizzes used to populate the student dashboard.</p>
 */
public class StudentDashboardResponse {
    private Long studentUserId;
    private int enrolledCourseCount;
    private List<CourseDto> enrolledCourses;
    private List<QuizResultDto> recentQuizResults;
    private List<QuizSummaryDto> upcomingQuizzes;

    public Long getStudentUserId() { return studentUserId; }
    public void setStudentUserId(Long studentUserId) { this.studentUserId = studentUserId; }

    public int getEnrolledCourseCount() { return enrolledCourseCount; }
    public void setEnrolledCourseCount(int enrolledCourseCount) { this.enrolledCourseCount = enrolledCourseCount; }

    public List<CourseDto> getEnrolledCourses() { return enrolledCourses; }
    public void setEnrolledCourses(List<CourseDto> enrolledCourses) { this.enrolledCourses = enrolledCourses; }

    public List<QuizResultDto> getRecentQuizResults() { return recentQuizResults; }
    public void setRecentQuizResults(List<QuizResultDto> recentQuizResults) { this.recentQuizResults = recentQuizResults; }

    public List<QuizSummaryDto> getUpcomingQuizzes() { return upcomingQuizzes; }
    public void setUpcomingQuizzes(List<QuizSummaryDto> upcomingQuizzes) { this.upcomingQuizzes = upcomingQuizzes; }
}
