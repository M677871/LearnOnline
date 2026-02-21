package com.csis231.api.category;

import com.csis231.api.common.BadRequestException;
import com.csis231.api.common.ConflictException;
import com.csis231.api.common.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service layer for managing {@link Category} entities.
 *
 * <p>Provides simple CRUD operations and enforces the unique category
 * name constraint.</p>
 */
@Service
public class CategoryService {

    private final CategoryRepository repo;

    /**
     * Creates a new {@code CategoryService} with the given repository.
     *
     * @param repo the repository used to access categories
     */
    public CategoryService(CategoryRepository repo) {
        this.repo = repo;
    }

    /**
     * Returns all categories.
     *
     * @return list of all {@link Category} entities
     */
    @Transactional(readOnly = true)
    public List<Category> list() {
        return repo.findAll();
    }

    /**
     * Returns a paginated list of categories.
     *
     * @param pageable paging information (page/size)
     * @return a {@link Page} of categories
     */
    @Transactional(readOnly = true)
    public Page<Category> list(Pageable pageable) {
        return repo.findAll(pageable);
    }

    /**
     * Retrieves a category by its identifier.
     *
     * @param id the category ID
     * @return the matching {@link Category}
     * @throws java.util.NoSuchElementException if no category with this ID exists
     */
    @Transactional(readOnly = true)
    public Category get(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));
    }

    /**
     * Creates a new category.
     *
     * @param c the category to create
     * @return the persisted {@link Category}
     * @throws IllegalArgumentException if a category with the same name already exists
     */
    @Transactional
    public Category create(Category c) {
        if (c == null || c.getName() == null || c.getName().isBlank()) {
            throw new BadRequestException("Category name is required");
        }
        if (repo.existsByName(c.getName())) {
            throw new ConflictException("Duplicate category");
        }
        c.setName(c.getName().trim());
        return repo.save(c);
    }

    /**
     * Updates the name of an existing category.
     *
     * @param id      the identifier of the category to update
     * @param updated an object containing the new name
     * @return the updated {@link Category}
     * @throws java.util.NoSuchElementException if no category with this ID exists
     */
    @Transactional
    public Category update(Long id, Category updated) {
        if (updated == null || updated.getName() == null || updated.getName().isBlank()) {
            throw new BadRequestException("Category name is required");
        }
        Category c = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));

        String newName = updated.getName().trim();
        if (!newName.equalsIgnoreCase(c.getName()) && repo.existsByName(newName)) {
            throw new ConflictException("Duplicate category");
        }

        c.setName(newName);
        return repo.save(c);
    }

    /**
     * Deletes a category by its identifier.
     *
     * @param id the ID of the category to delete
     */
    @Transactional
    public void delete(Long id) {
        if (!repo.existsById(id)) {
            throw new ResourceNotFoundException("Category not found: " + id);
        }
        repo.deleteById(id);
    }
}
