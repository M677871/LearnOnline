package com.example.demo.common;

import com.example.demo.model.MeResponse;
import com.example.demo.model.CourseDto;
import com.example.demo.model.QuizSummaryDto;

/**
 * Simple in-memory holder for frequently reused session-scoped values.
 */
public final class SessionStore {
    private static MeResponse me;
    private static CourseDto activeCourse;
    private static QuizSummaryDto activeQuiz;

    private SessionStore() {}

    public static MeResponse getMe() { return me; }
    public static void setMe(MeResponse meResponse) { me = meResponse; }

    public static CourseDto getActiveCourse() { return activeCourse; }
    public static void setActiveCourse(CourseDto course) { activeCourse = course; }

    public static QuizSummaryDto getActiveQuiz() { return activeQuiz; }
    public static void setActiveQuiz(QuizSummaryDto quiz) { activeQuiz = quiz; }

    /**
     * Returns the current user's role in uppercase (ADMIN/INSTRUCTOR/STUDENT) or null.
     */
    public static String currentRole() {
        return me != null && me.getRole() != null ? me.getRole().toUpperCase() : null;
    }

    public static void clearAll() {
        me = null;
        activeCourse = null;
        activeQuiz = null;
    }
}
