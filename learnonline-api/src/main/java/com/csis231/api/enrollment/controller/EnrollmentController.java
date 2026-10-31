package com.csis231.api.enrollment.controller;

import com.csis231.api.enrollment.dto.EnrollmentRequest;
import com.csis231.api.enrollment.dto.EnrollmentResponse;
import com.csis231.api.enrollment.service.EnrollmentService;
import com.csis231.api.user.service.AuthenticatedUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class EnrollmentController {

    private final EnrollmentService enrollmentService;
    private final AuthenticatedUserService authenticatedUserService;

    @PostMapping("/enrollments/enroll")
    public EnrollmentResponse enroll(@Valid @RequestBody EnrollmentRequest request, Authentication authentication) {
        return enrollmentService.enroll(authenticatedUserService.require(authentication), request);
    }

    @GetMapping("/students/{userId}/enrollments")
    public List<EnrollmentResponse> enrollmentsForStudent(@PathVariable Long userId, Authentication authentication) {
        return enrollmentService.listForStudent(userId, authenticatedUserService.require(authentication));
    }

    @GetMapping("/courses/{courseId}/enrollments")
    public List<EnrollmentResponse> enrollmentsForCourse(@PathVariable Long courseId, Authentication authentication) {
        return enrollmentService.listForCourse(courseId, authenticatedUserService.require(authentication));
    }
}
