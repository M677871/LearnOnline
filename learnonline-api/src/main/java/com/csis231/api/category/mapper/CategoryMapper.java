package com.csis231.api.category.mapper;

import com.csis231.api.category.dto.CategoryDto;
import com.csis231.api.category.model.Category;

public final class CategoryMapper {

    private CategoryMapper() {
    }

    public static CategoryDto toDto(Category category) {
        return new CategoryDto(category.getId(), category.getName());
    }
}
