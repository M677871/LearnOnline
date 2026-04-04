package com.csis231.api.coursematerial.controller;

import com.csis231.api.common.exception.ResourceNotFoundException;
import com.csis231.api.common.exception.UnauthorizedException;
import com.csis231.api.common.service.AuthenticatedUserService;
import com.csis231.api.coursematerial.mapper.CourseMaterialMapper;
import com.csis231.api.coursematerial.dto.CourseMaterialDto;
import com.csis231.api.coursematerial.dto.CourseMaterialRequest;
import com.csis231.api.coursematerial.model.CourseMaterial;
import com.csis231.api.coursematerial.service.CourseMaterialService;
import com.csis231.api.user.model.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST endpoints for course materials.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CourseMaterialController {
    private final CourseMaterialService materialService;
    private final AuthenticatedUserService authenticatedUserService;

    /**
     * Lists materials for a course that are visible to the current user.
     *
     * @param courseId       the course identifier
     * @param authentication the authenticated principal requesting the data
     * @return a list of {@link CourseMaterialDto} visible to the viewer
     */
    @GetMapping("/courses/{courseId}/materials")
    public List<CourseMaterialDto> list(@PathVariable Long courseId, Authentication authentication) {
        User viewer = authenticatedUserService.require(authentication);
        return materialService.mapToDto(materialService.listForViewer(courseId, viewer));
    }

    /**
     * Creates a new course material under the specified course.
     *
     * @param courseId       the course identifier
     * @param request        the material payload
     * @param authentication the authenticated principal performing the operation
     * @return {@code 201 Created} with the created {@link CourseMaterialDto}
     */
    @PostMapping("/courses/{courseId}/materials")
    public ResponseEntity<CourseMaterialDto> create(@PathVariable Long courseId,
                                                    @Valid @RequestBody CourseMaterialRequest request,
                                                    Authentication authentication) {
        User actor = authenticatedUserService.require(authentication);
        CourseMaterial created = materialService.addMaterial(courseId, request, actor);
        return ResponseEntity.status(201).body(CourseMaterialMapper.toDto(created));
    }

    /**
     * Deletes a material by id after verifying permissions.
     *
     * @param id             the material identifier
     * @param authentication the authenticated principal
     * @return {@link ResponseEntity} with no content on success
     */
    @DeleteMapping("/materials/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication authentication) {
        User actor = authenticatedUserService.require(authentication);
        materialService.deleteMaterial(id, actor);
        return ResponseEntity.noContent().build();
    }
}
