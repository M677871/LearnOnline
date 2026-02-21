package com.csis231.api.category;

import com.csis231.api.common.BadRequestException;
import com.csis231.api.common.PagedResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller exposing CRUD endpoints for {@link Category} entities.
 *
 * <p>Base path: {@code /api/categories}</p>
 */

@RestController
@RequestMapping("/api/categories")
public class CategoryController
{
    private final CategoryService svc;

    /**
     * Creates a new {@code CategoryController} with the given service.
     *
     * @param svc the {@link CategoryService} to delegate to
     */

    public CategoryController(CategoryService svc)
    {
        this.svc = svc;
    }

    /**
     * Lists all categories.
     *
     * @return list of all {@link Category} entities
     */

    @GetMapping
    public PagedResponse<Category> list(@RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "10") int size) {
        if (size <= 0) {
            throw new BadRequestException("Size must be greater than zero");
        }

        var springPage = svc.list(PageRequest.of(Math.max(0, page), size));
        return PagedResponse.fromPage(springPage);
    }
    /**
     * Retrieves a single category by its identifier.
     *
     * @param id the ID of the category
     * @return the matching {@link Category}
     */

    @GetMapping("/{id}") public Category get(@PathVariable Long id)
    {
        return svc.get(id);
    }

    /**
     * Creates a new category.
     *
     * @param category the category to create
     * @return the persisted {@link Category}
     */

    @PostMapping public Category create(@RequestBody Category category)
    {
        return svc.create(category);
    }

    /**
     * Updates an existing category.
     *
     * @param id       the ID of the category to update
     * @param category an object containing the new name
     * @return the updated {@link Category}
     */

    @PutMapping("/{id}") public Category update(@PathVariable Long id, @RequestBody Category category)
    {
        return svc.update(id, category);
    }

    /**
     * Deletes a category by its identifier.
     *
     * @param id the ID of the category to delete
     */

    @DeleteMapping("/{id}") public void delete(@PathVariable Long id)
    {
        svc.delete(id);
    }
}
