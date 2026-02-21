package com.csis231.api.enrollment;

import com.csis231.api.common.ResourceNotFoundException;
import com.csis231.api.common.UnauthorizedException;
import com.csis231.api.user.User;
import com.csis231.api.user.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST endpoints for managing course enrollments.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class EnrollmentController {
    private final EnrollmentService enrollmentService;
    private final UserRepository userRepository;

    /**
     * Enrolls a student (self or target) into a course.
     *
     * @param request        the enrollment request with courseId and optional studentUserId
     * @param authentication the authenticated principal performing the action
     * @return the created {@link EnrollmentResponse} representing the enrollment
     */
    @PostMapping("/enrollments/enroll")
    public EnrollmentResponse enroll(@Valid @RequestBody EnrollmentRequest request,
                                     Authentication authentication) {
        User actor = resolveUser(authentication);
        return EnrollmentMapper.toDto(enrollmentService.enroll(actor, request));
    }

    /**
     * Lists enrollments for a given student. Admins/instructors may view others; students may view themselves.
     *
     * @param userId         the student user id
     * @param authentication the authenticated principal requesting the data
     * @return a list of enrollments for the specified student
     */
    @GetMapping("/students/{userId}/enrollments")
    public List<EnrollmentResponse> enrollmentsForStudent(@PathVariable Long userId,
                                                          Authentication authentication) {
        User actor = resolveUser(authentication);
        if (!actor.getId().equals(userId) && actor.getRole() != User.Role.ADMIN && actor.getRole() != User.Role.INSTRUCTOR) {
            throw new UnauthorizedException("You cannot view enrollments for another student");
        }
        return enrollmentService.findByStudent(userId).stream()
                .map(EnrollmentMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Lists enrollments for a course, enforcing instructor/admin visibility rules.
     *
     * @param courseId       the course identifier
     * @param authentication the authenticated principal
     * @return a list of enrollments for the course
     */
    @GetMapping("/courses/{courseId}/enrollments")
    public List<EnrollmentResponse> enrollmentsForCourse(@PathVariable Long courseId, Authentication authentication) {
        User actor = resolveUser(authentication);
        return enrollmentService.findByCourse(courseId, actor).stream()
                .map(EnrollmentMapper::toDto)
                .collect(Collectors.toList());
    }

    private User resolveUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new UnauthorizedException("Authentication required");
        }
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + authentication.getName()));
    }
}
