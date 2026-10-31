package com.csis231.api.coursematerial.mapper;

import com.csis231.api.coursematerial.dto.CourseMaterialDto;
import com.csis231.api.coursematerial.model.CourseMaterial;

public final class CourseMaterialMapper {

    private CourseMaterialMapper() {
    }

    public static CourseMaterialDto toDto(CourseMaterial material) {
        return new CourseMaterialDto(
                material.getId(),
                material.getCourse().getId(),
                material.getTitle(),
                material.getMaterialType(),
                material.getUrl(),
                material.getMetadata(),
                material.getCreatedAt()
        );
    }
}
