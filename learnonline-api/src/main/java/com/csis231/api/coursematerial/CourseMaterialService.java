package com.csis231.api.coursematerial;

import com.csis231.api.common.BadRequestException;
import com.csis231.api.common.ResourceNotFoundException;
import com.csis231.api.common.UnauthorizedException;
import com.csis231.api.course.Course;
import com.csis231.api.course.CourseRepository;
import com.csis231.api.enrollment.EnrollmentService;
import com.csis231.api.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Service for managing course materials and enforcing access rules.
 */
@Service
@RequiredArgsConstructor
public class CourseMaterialService {
    private final CourseMaterialRepository materialRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentService enrollmentService;

    /**
     * Adds a new course material for the specified course.
     *
     * @param courseId the course identifier
     * @param req      the material payload
     * @param actor    the authenticated user performing the operation
     * @return the persisted {@link CourseMaterial}
     * @throws BadRequestException      if required fields are missing
     * @throws ResourceNotFoundException if the course does not exist
     * @throws UnauthorizedException    if the actor is not allowed to modify the course
     */
    @Transactional
    public CourseMaterial addMaterial(Long courseId, CourseMaterialRequest req, User actor) {
        if (courseId == null) throw new BadRequestException("courseId is required");
        if (req == null) throw new BadRequestException("Material payload is required");
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found: " + courseId));
        requireOwnerOrAdmin(course, actor);

        CourseMaterial material = CourseMaterial.builder()
                .course(course)
                .title(req.title())
                .materialType(req.materialType())
                .url(req.url())
                .metadata(req.metadata())
                .build();
        return materialRepository.save(material);
    }

    /**
     * Deletes a material by id after verifying permissions.
     *
     * @param materialId the material id to delete
     * @param actor      the authenticated user requesting deletion
     * @throws ResourceNotFoundException if the material is not found
     * @throws UnauthorizedException    if the actor is not allowed to modify the course
     */
    @Transactional
    public void deleteMaterial(Long materialId, User actor) {
        CourseMaterial material = materialRepository.findById(materialId)
                .orElseThrow(() -> new ResourceNotFoundException("Material not found: " + materialId));
        requireOwnerOrAdmin(material.getCourse(), actor);
        materialRepository.delete(material);
    }

    /**
     * Lists materials visible to the given viewer. Students must be enrolled;
     * instructors and admins can view regardless of enrollment. If viewer is
     * {@code null}, materials are hidden.
     *
     * @param courseId the course identifier
     * @param viewer   the requesting user (may be null)
     * @return list of materials visible to the viewer
     * @throws ResourceNotFoundException if the course is not found
     * @throws UnauthorizedException    if the viewer is not allowed to view
     */
    @Transactional(readOnly = true)
    public List<CourseMaterial> listForViewer(Long courseId, User viewer) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found: " + courseId));
        if (viewer == null) {
            return Collections.emptyList();
        }
        if (viewer.getRole() == User.Role.ADMIN) {
            return materialRepository.findByCourse_Id(courseId);
        }
        if (viewer.getRole() == User.Role.INSTRUCTOR) {
            if (course.getInstructor() != null && Objects.equals(course.getInstructor().getId(), viewer.getId())) {
                return materialRepository.findByCourse_Id(courseId);
            }
            throw new UnauthorizedException("You cannot view materials of courses you do not own");
        }
        if (viewer.getRole() == User.Role.STUDENT) {
            boolean enrolled = enrollmentService.isStudentEnrolled(viewer.getId(), courseId);
            if (!enrolled) {
                throw new UnauthorizedException("Enroll in the course to view materials");
            }
            return materialRepository.findByCourse_Id(courseId);
        }
        return Collections.emptyList();
    }

    /**
     * Maps a list of course materials to DTOs.
     *
     * @param materials the source materials
     * @return list of {@link CourseMaterialDto} representations
     */
    public List<CourseMaterialDto> mapToDto(List<CourseMaterial> materials) {
        return materials.stream().map(CourseMaterialMapper::toDto).collect(Collectors.toList());
    }

    private static void requireOwnerOrAdmin(Course course, User actor) {
        if (actor == null) throw new UnauthorizedException("Authentication required");
        if (actor.getRole() == User.Role.ADMIN) return;
        if (actor.getRole() != User.Role.INSTRUCTOR) {
            throw new UnauthorizedException("Only instructors can modify materials");
        }
        if (course.getInstructor() == null || !Objects.equals(course.getInstructor().getId(), actor.getId())) {
            throw new UnauthorizedException("You can only modify materials for your own courses");
        }
    }
}
