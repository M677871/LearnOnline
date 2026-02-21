package com.example.demo.admin;

import com.example.demo.common.ApiClient;
import com.example.demo.common.PageResponse;
import com.example.demo.model.User;
import com.fasterxml.jackson.core.type.TypeReference;

/**
 * API client for user management endpoints.
 */
public class UserApi {
    private final ApiClient client = new ApiClient();

    /**
     * Retrieves a paginated list of users.
     *
     * @param page the zero-based page index
     * @param size the number of users per page
     * @return a {@link PageResponse} containing users and metadata
     */
    public PageResponse<User> list(int page, int size) {
        String path = "/api/csis-users?page=" + page + "&size=" + size;
        return client.getPage(path, User.class);
    }

    /**
     * Finds first user whose username or email matches the given identifier.
     * Scans first 200 results to avoid backend changes.
     */
    public java.util.Optional<User> findByIdentifier(String identifier) {
        if (identifier == null || identifier.isBlank()) return java.util.Optional.empty();
        try {
            PageResponse<User> page = client.getPage("/api/csis-users?page=0&size=200", User.class);
            if (page != null && page.getContent() != null) {
                return page.getContent().stream()
                        .filter(u -> identifier.equalsIgnoreCase(u.getUsername()) || identifier.equalsIgnoreCase(u.getEmail()))
                        .findFirst();
            }
        } catch (Exception ignored) {}
        return java.util.Optional.empty();
    }

    /**
     * Returns a filtered list of instructors (role = INSTRUCTOR) from the first page.
     *
     * @param pageSize the number of users to fetch from the first page
     * @return list of instructor users
     */
    public java.util.List<User> listInstructors(int pageSize) {
        PageResponse<User> page = client.getPage("/api/csis-users?page=0&size=" + pageSize, User.class);
        if (page != null && page.getContent() != null) {
            return page.getContent().stream()
                    .filter(u -> "INSTRUCTOR".equalsIgnoreCase(u.getRole()))
                    .toList();
        }
        return java.util.List.of();
    }


    /**
     * Retrieves a user by identifier.
     *
     * @param id the user id
     * @return the matching {@link User}
     */
    public User get(Long id) {
        return client.get("/api/csis-users/" + id, new TypeReference<User>() {}).getBody();
    }

    /**
     * Creates a new user.
     *
     * @param user the user payload
     * @return the created {@link User}
     */
    public User create(User user) {
        return client.post("/api/csis-users", user, new TypeReference<User>() {}).getBody();
    }

    /**
     * Updates an existing user; the user id must be present.
     *
     * @param user the user payload with id
     * @return the updated {@link User}
     */
    public User update(User user) {
        if (user.getId() == null) {
            throw new IllegalArgumentException("User id must not be null for update");
        }
        return client.put("/api/csis-users/" + user.getId(), user, new TypeReference<User>() {}).getBody();
    }

    /**
     * Deletes a user by identifier.
     *
     * @param id the user id
     */
    public void delete(Long id) {
        client.delete("/api/csis-users/" + id);
    }
}
