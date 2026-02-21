package com.example.demo.admin;

import com.example.demo.common.ApiClient;
import com.example.demo.common.PageResponse;
import com.example.demo.model.Category;
import com.fasterxml.jackson.core.type.TypeReference;

/**
 * API client for category management.
 */
public class CategoryApi {
    private final ApiClient client = new ApiClient();

    /**
     * Retrieves a paginated list of categories.
     *
     * @param page the zero-based page index
     * @param size the number of categories per page
     * @return a {@link PageResponse} of {@link Category} items
     */
    public PageResponse<Category> list(int page, int size) {
        String path = "/api/categories?page=" + page + "&size=" + size;
        return client.getPage(path, Category.class);
    }


    /**
     * Creates a new category with the given name.
     *
     * @param name the category name to create
     * @return the created {@link Category}
     */
    public Category create(String name) {
        Category body = new Category();
        body.setName(name);
        return client.post("/api/categories", body, new TypeReference<Category>() {}).getBody();
    }

    /**
     * Updates the name of an existing category.
     *
     * @param id   the category identifier
     * @param name the new name to apply
     * @return the updated {@link Category}
     */
    public Category update(Long id, String name) {
        Category body = new Category();
        body.setName(name);
        return client.put("/api/categories/" + id, body, new TypeReference<Category>() {}).getBody();
    }

    /**
     * Deletes a category by identifier.
     *
     * @param id the category identifier
     */
    public void delete(Long id) {
        client.delete("/api/categories/" + id);
    }
}
