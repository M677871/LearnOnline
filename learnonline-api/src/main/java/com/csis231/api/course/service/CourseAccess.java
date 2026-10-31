package com.csis231.api.course.service;

import com.csis231.api.common.exception.ForbiddenException;
import com.csis231.api.course.model.Course;
import com.csis231.api.user.model.User;

/**
 * The one definition of who may manage a course, shared by every feature that hangs off a course.
 * Callers supply the messages because each feature describes the refusal in its own terms.
 */
public final class CourseAccess {

    private CourseAccess() {
    }

    public static void requireStaff(User actor, String message) {
        if (!actor.isAdmin() && !actor.isInstructor()) {
            throw new ForbiddenException(message);
        }
    }

    public static void requireManager(Course course, User actor, String notStaffMessage, String notOwnerMessage) {
        requireStaff(actor, notStaffMessage);
        if (!course.isManagedBy(actor)) {
            throw new ForbiddenException(notOwnerMessage);
        }
    }
}
