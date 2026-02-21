package com.csis231.api.category;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for {@link Category} entities.
 */
public interface CategoryRepository extends JpaRepository<Category, Long> {

    /**
     * Checks whether a category with the given name already exists.
     *
     * @param name the category name to check
     * @return {@code true} if a category with this name exists; {@code false} otherwise
     */
    boolean existsByName(String name);
}
