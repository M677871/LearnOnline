package com.csis231.api.coursematerial;

import com.csis231.api.common.ResourceNotFoundException;
import com.csis231.api.common.UnauthorizedException;
import com.csis231.api.user.User;
import com.csis231.api.user.UserRepository;
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
    private final UserRepository userRepository;

    /**
     * Lists materials for a course that are visible to the current user.
     *
     * @param courseId       the course identifier
     * @param authentication the authenticated principal requesting the data
     * @return a list of {@link CourseMaterialDto} visible to the viewer
     */
    @GetMapping("/courses/{courseId}/materials")
    public List<CourseMaterialDto> list(@PathVariable Long courseId, Authentication authentication) {
        User viewer = resolveUser(authentication);
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
        User actor = resolveUser(authentication);
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
        User actor = resolveUser(authentication);
        materialService.deleteMaterial(id, actor);
        return ResponseEntity.noContent().build();
    }

    private User resolveUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new UnauthorizedException("Authentication required");
        }
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + authentication.getName()));
    }
}
