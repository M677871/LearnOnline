package com.csis231.api.enrollment.service;

import com.csis231.api.common.exception.ConflictException;
import com.csis231.api.common.exception.ForbiddenException;
import com.csis231.api.common.exception.ResourceNotFoundException;
import com.csis231.api.course.dto.CourseDto;
import com.csis231.api.course.mapper.CourseMapper;
import com.csis231.api.course.model.Course;
import com.csis231.api.course.repository.CourseRepository;
import com.csis231.api.course.service.CourseAccess;
import com.csis231.api.enrollment.dto.EnrollmentRequest;
import com.csis231.api.enrollment.dto.EnrollmentResponse;
import com.csis231.api.enrollment.mapper.EnrollmentMapper;
import com.csis231.api.enrollment.model.CourseEnrollment;
import com.csis231.api.enrollment.repository.CourseEnrollmentRepository;
import com.csis231.api.user.model.User;
import com.csis231.api.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EnrollmentService {

    private final CourseEnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;

    /**
     * Enrolls the actor, or the given student when the actor is an admin. Users of any role can be
     * enrolled, so instructors can take other instructors' courses.
     */
    @Transactional
    public EnrollmentResponse enroll(User actor, EnrollmentRequest req) {
        Long studentId = req.studentUserId() != null ? req.studentUserId() : actor.getId();
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentId));
        if (!actor.getId().equals(studentId) && !actor.isAdmin()) {
            throw new ForbiddenException("You cannot enroll another user");
        }

        Course course = courseRepository.getOrThrow(req.courseId());
        if (Boolean.FALSE.equals(course.getPublished())) {
            throw new ForbiddenException("Cannot enroll in an unpublished course");
        }
        if (isStudentEnrolled(studentId, req.courseId())) {
            throw new ConflictException("Already enrolled in this course");
        }

        return EnrollmentMapper.toDto(enrollmentRepository.save(CourseEnrollment.builder()
                .course(course)
                .student(student)
                .build()));
    }

    /**
     * Students see their own enrollments; instructors and admins can see anyone's.
     */
    @Transactional(readOnly = true)
    public List<EnrollmentResponse> listForStudent(Long studentId, User actor) {
        if (!actor.getId().equals(studentId) && actor.isStudent()) {
            throw new ForbiddenException("You cannot view enrollments for another student");
        }
        return enrollmentRepository.findByStudent_Id(studentId).stream()
                .map(EnrollmentMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EnrollmentResponse> listForCourse(Long courseId, User actor) {
        CourseAccess.requireManager(courseRepository.getOrThrow(courseId), actor,
                "Only instructors or admins can view course enrollments",
                "You can only view enrollments for your courses");
        return enrollmentRepository.findByCourse_Id(courseId).stream()
                .map(EnrollmentMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CourseDto> enrolledCourses(Long studentId) {
        return enrollmentRepository.findByStudent_Id(studentId).stream()
                .map(CourseEnrollment::getCourse)
                .map(CourseMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public long countForCourse(Long courseId) {
        return enrollmentRepository.countByCourse_Id(courseId);
    }

    @Transactional(readOnly = true)
    public boolean isStudentEnrolled(Long studentId, Long courseId) {
        return enrollmentRepository.existsByStudent_IdAndCourse_Id(studentId, courseId);
    }
}
