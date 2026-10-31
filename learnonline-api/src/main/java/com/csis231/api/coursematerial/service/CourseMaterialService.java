package com.csis231.api.coursematerial.service;

import com.csis231.api.common.exception.ForbiddenException;
import com.csis231.api.common.exception.ResourceNotFoundException;
import com.csis231.api.course.model.Course;
import com.csis231.api.course.repository.CourseRepository;
import com.csis231.api.course.service.CourseAccess;
import com.csis231.api.coursematerial.dto.CourseMaterialDto;
import com.csis231.api.coursematerial.dto.CourseMaterialRequest;
import com.csis231.api.coursematerial.mapper.CourseMaterialMapper;
import com.csis231.api.coursematerial.model.CourseMaterial;
import com.csis231.api.coursematerial.repository.CourseMaterialRepository;
import com.csis231.api.enrollment.service.EnrollmentService;
import com.csis231.api.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CourseMaterialService {

    private final CourseMaterialRepository materialRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentService enrollmentService;

    @Transactional
    public CourseMaterialDto addMaterial(Long courseId, CourseMaterialRequest req, User actor) {
        Course course = courseRepository.getOrThrow(courseId);
        requireManager(course, actor);

        return CourseMaterialMapper.toDto(materialRepository.save(CourseMaterial.builder()
                .course(course)
                .title(req.title())
                .materialType(req.materialType())
                .url(req.url())
                .metadata(req.metadata())
                .build()));
    }

    @Transactional
    public void deleteMaterial(Long materialId, User actor) {
        CourseMaterial material = materialRepository.findById(materialId)
                .orElseThrow(() -> new ResourceNotFoundException("Material not found: " + materialId));
        requireManager(material.getCourse(), actor);
        materialRepository.delete(material);
    }

    /**
     * Lists a course's materials for a viewer. Anonymous viewers ({@code null}) get an empty list;
     * signed-in users who are neither enrolled students nor managers of the course are refused.
     */
    @Transactional(readOnly = true)
    public List<CourseMaterialDto> listForViewer(Long courseId, User viewer) {
        Course course = courseRepository.getOrThrow(courseId);
        if (viewer == null) {
            return List.of();
        }
        requireCanView(course, viewer);
        return materialRepository.findByCourse_Id(courseId).stream()
                .map(CourseMaterialMapper::toDto)
                .toList();
    }

    private void requireCanView(Course course, User viewer) {
        if (viewer.isStudent()) {
            if (!enrollmentService.isStudentEnrolled(viewer.getId(), course.getId())) {
                throw new ForbiddenException("Enroll in the course to view materials");
            }
        } else if (!course.isManagedBy(viewer)) {
            throw new ForbiddenException("You cannot view materials of courses you do not own");
        }
    }

    private static void requireManager(Course course, User actor) {
        CourseAccess.requireManager(course, actor,
                "Only instructors can modify materials",
                "You can only modify materials for your own courses");
    }
}
