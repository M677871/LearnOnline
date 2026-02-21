package com.csis231.api.user;

import com.csis231.api.common.BadRequestException;
import com.csis231.api.common.PagedResponse;
import com.csis231.api.common.ResourceNotFoundException;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller exposing CRUD endpoints for {@link User} entities.
 * The base path /api/csis-users avoids clashing with the standard
 * LearnOnline user controller already present in the application.
 */
@RestController
@RequestMapping("/api/csis-users")
public class UserController {
    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Retrieves a paginated list of users.
     *
     * @param page the zero-based page index to return
     * @param size the number of users per page (must be greater than zero)
     * @return a {@link PagedResponse} containing users and pagination metadata
     */
    @GetMapping
    public PagedResponse<User> list(@RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "10") int size) {
        if (size <= 0) {
            throw new BadRequestException("Size must be greater than zero");
        }

        var springPage = userService.getUsers(PageRequest.of(Math.max(0, page), size));
        return PagedResponse.fromPage(springPage);
    }
    /**
     * Retrieves a user by identifier.
     *
     * @param id the user ID to fetch
     * @return the matching {@link User}
     */
    @GetMapping("/{id}")
    public User get(@PathVariable Long id) {
        return userService.getUser(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }

    /**
     * Creates a new user record.
     *
     * @param user the user payload to persist
     * @return the created {@link User}
     */
    @PostMapping
    public ResponseEntity<User> create(@Valid @RequestBody User user) {
        User created = userService.createUser(user);
        return ResponseEntity.status(201).body(created);
    }

    /**
     * Updates an existing user, applying only non-null fields.
     *
     * @param id   the identifier of the user to update
     * @param user the incoming user fields to apply
     * @return the updated {@link User}
     */
    @PutMapping("/{id}")
    public User update(@PathVariable Long id,
                                       @Valid @RequestBody User user) {
        return userService.updateUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }

    /**
     * Deletes a user by identifier.
     *
     * @param id the user ID to delete
     * @return {@link ResponseEntity} with no content on success
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Returns profile information about the currently authenticated user.
     *
     * <p>The username is taken from the {@link Authentication} object and the
     * corresponding {@link User} is mapped to a {@link com.csis231.api.user.MeResponse}
     * DTO that is safe to expose to the frontend.</p>
     *
     * @param authentication the Spring Security authentication of the current request
     * @return a {@link com.csis231.api.user.MeResponse} containing basic user data
     */

    @GetMapping("/me")
    public com.csis231.api.user.MeResponse me(Authentication authentication) {
        String username = authentication.getName();
        User u = userService.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found: " + username));

        return new com.csis231.api.user.MeResponse(
                u.getId(),
                u.getUsername(),
                u.getEmail(),
                u.getFirstName(),
                u.getLastName(),
                u.getPhone(),
                u.getRole().name()
        );
    }


}

