package com.csis231.api.coursematerial.controller;

import com.csis231.api.coursematerial.dto.CourseMaterialDto;
import com.csis231.api.coursematerial.dto.CourseMaterialRequest;
import com.csis231.api.coursematerial.service.CourseMaterialService;
import com.csis231.api.user.service.AuthenticatedUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CourseMaterialController {

    private final CourseMaterialService materialService;
    private final AuthenticatedUserService authenticatedUserService;

    @GetMapping("/courses/{courseId}/materials")
    public List<CourseMaterialDto> list(@PathVariable Long courseId, Authentication authentication) {
        return materialService.listForViewer(courseId, authenticatedUserService.require(authentication));
    }

    @PostMapping("/courses/{courseId}/materials")
    @ResponseStatus(HttpStatus.CREATED)
    public CourseMaterialDto create(@PathVariable Long courseId,
                                    @Valid @RequestBody CourseMaterialRequest request,
                                    Authentication authentication) {
        return materialService.addMaterial(courseId, request, authenticatedUserService.require(authentication));
    }

    @DeleteMapping("/materials/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, Authentication authentication) {
        materialService.deleteMaterial(id, authenticatedUserService.require(authentication));
    }
}
