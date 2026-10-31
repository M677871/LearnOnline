package com.csis231.api.category.repository;

import com.csis231.api.category.model.Category;
import com.csis231.api.common.exception.ResourceNotFoundException;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    boolean existsByName(String name);

    default Category getOrThrow(Long id) {
        return findById(id).orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));
    }
}
