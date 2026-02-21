package com.csis231.api.enrollment;

import com.csis231.api.common.BadRequestException;
import com.csis231.api.common.ConflictException;
import com.csis231.api.common.ResourceNotFoundException;
import com.csis231.api.common.UnauthorizedException;
import com.csis231.api.course.Course;
import com.csis231.api.course.CourseRepository;
import com.csis231.api.user.User;
import com.csis231.api.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service handling enrollment flows.
 */
@Service
@RequiredArgsConstructor
public class EnrollmentService {
    private final CourseEnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;

    /**
     * Enrolls the target student (or the actor) into the given course after validation.
     *
     * @param actor the authenticated user initiating the enrollment
     * @param req   the enrollment request containing courseId and optional studentUserId
     * @return the created {@link CourseEnrollment}
     * @throws BadRequestException      if payload is missing required fields
     * @throws UnauthorizedException    if the actor cannot enroll the target
     * @throws ResourceNotFoundException if the course or student cannot be found
     * @throws ConflictException        if the enrollment already exists
     */
    @Transactional
    public CourseEnrollment enroll(User actor, EnrollmentRequest req) {
        if (req == null || req.courseId() == null) {
            throw new BadRequestException("courseId is required");
        }
        if (actor == null) throw new UnauthorizedException("Authentication required");

        Long targetStudentId = req.studentUserId() != null ? req.studentUserId() : actor.getId();
        User targetStudent = userRepository.findById(targetStudentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + targetStudentId));

        // only the same user or an admin can enroll this target
        boolean isSelf = actor.getId().equals(targetStudentId);
        if (!isSelf && actor.getRole() != User.Role.ADMIN) {
            throw new UnauthorizedException("You cannot enroll another user");
        }
        // allow STUDENT / INSTRUCTOR / ADMIN to be enrolled (admin already allowed)
        if (targetStudent.getRole() == null) {
            throw new UnauthorizedException("User role is required for enrollment");
        }

        Course course = courseRepository.findById(req.courseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course not found: " + req.courseId()));
        if (Boolean.FALSE.equals(course.getPublished())) {
            throw new UnauthorizedException("Cannot enroll in an unpublished course");
        }

        if (enrollmentRepository.existsByStudent_IdAndCourse_Id(targetStudentId, req.courseId())) {
            throw new ConflictException("Already enrolled in this course");
        }

        CourseEnrollment enrollment = CourseEnrollment.builder()
                .course(course)
                .student(targetStudent)
                .status(CourseEnrollment.EnrollmentStatus.ENROLLED)
                .build();
        return enrollmentRepository.save(enrollment);
    }

    /**
     * Lists enrollments for a given student.
     *
     * @param studentId the student identifier
     * @return list of {@link CourseEnrollment} for the student
     */
    @Transactional(readOnly = true)
    public List<CourseEnrollment> findByStudent(Long studentId) {
        return enrollmentRepository.findByStudent_Id(studentId);
    }

    /**
     * Lists enrollments for a given course, enforcing instructor/admin visibility rules.
     *
     * @param courseId the course identifier
     * @param actor    the authenticated user requesting the data
     * @return list of {@link CourseEnrollment} for the course
     * @throws UnauthorizedException    if the actor is not allowed to view
     * @throws ResourceNotFoundException if the course is not found
     */
    @Transactional(readOnly = true)
    public List<CourseEnrollment> findByCourse(Long courseId, User actor) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found: " + courseId));

        if (actor == null) throw new UnauthorizedException("Authentication required");
        if (actor.getRole() == User.Role.ADMIN) {
            return enrollmentRepository.findByCourse_Id(courseId);
        }
        if (actor.getRole() == User.Role.INSTRUCTOR) {
            if (course.getInstructor() != null && course.getInstructor().getId() != null
                    && course.getInstructor().getId().equals(actor.getId())) {
                return enrollmentRepository.findByCourse_Id(courseId);
            }
            throw new UnauthorizedException("You can only view enrollments for your courses");
        }
        throw new UnauthorizedException("Only instructors or admins can view course enrollments");
    }

    /**
     * Counts enrollments for a course.
     *
     * @param courseId the course identifier
     * @return the total number of enrollments
     */
    @Transactional(readOnly = true)
    public long countForCourse(Long courseId) {
        return enrollmentRepository.countByCourse_Id(courseId);
    }

    /**
     * Checks whether a student is enrolled in a course.
     *
     * @param studentId the student identifier
     * @param courseId  the course identifier
     * @return true if the enrollment exists
     */
    @Transactional(readOnly = true)
    public boolean isStudentEnrolled(Long studentId, Long courseId) {
        return enrollmentRepository.existsByStudent_IdAndCourse_Id(studentId, courseId);
    }
}
