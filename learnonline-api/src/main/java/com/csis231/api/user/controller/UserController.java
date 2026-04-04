package com.csis231.api.user.controller;


import com.csis231.api.common.dto.PagedResponse;
import com.csis231.api.common.exception.BadRequestException;
import com.csis231.api.common.exception.ResourceNotFoundException;
import com.csis231.api.common.service.AuthenticatedUserService;
import com.csis231.api.user.dto.MeResponse;
import com.csis231.api.user.dto.UserCreateRequest;
import com.csis231.api.user.dto.UserResponse;
import com.csis231.api.user.dto.UserUpdateRequest;
import com.csis231.api.user.model.User;
import com.csis231.api.user.service.UserService;
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
    private final AuthenticatedUserService authenticatedUserService;

    @Autowired
    public UserController(UserService userService, AuthenticatedUserService authenticatedUserService) {
        this.userService = userService;
        this.authenticatedUserService = authenticatedUserService;
    }

    /**
     * Retrieves a paginated list of users.
     *
     * @param page the zero-based page index to return
     * @param size the number of users per page (must be greater than zero)
    * @return a {@link PagedResponse} containing users and pagination metadata
     */
    @GetMapping
    public PagedResponse<UserResponse> list(@RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "10") int size) {
        if (size <= 0) {
            throw new BadRequestException("Size must be greater than zero");
        }

        var springPage = userService.getUsers(PageRequest.of(Math.max(0, page), size))
                .map(UserController::toResponse);
        return PagedResponse.fromPage(springPage);
    }
    /**
     * Retrieves a user by identifier.
     *
     * @param id the user ID to fetch
    * @return the matching user response DTO
     */
    @GetMapping("/{id}")
    public UserResponse get(@PathVariable Long id) {
        return userService.getUser(id)
                .map(UserController::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }

    /**
     * Creates a new user record.
     *
    * @param request the user payload to persist
    * @return the created user response DTO
     */
    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody UserCreateRequest request) {
        User created = userService.createUser(request);
        return ResponseEntity.status(201).body(toResponse(created));
    }

    /**
     * Updates an existing user, applying only non-null fields.
     *
     * @param id   the identifier of the user to update
     * @param request the incoming user fields to apply
     * @return the updated user response DTO
     */
    @PutMapping("/{id}")
    public UserResponse update(@PathVariable Long id,
                               @Valid @RequestBody UserUpdateRequest request) {
        return userService.updateUser(id, request)
                .map(UserController::toResponse)
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
     * corresponding {@link User} is mapped to a {@link MeResponse}
     * DTO that is safe to expose to the frontend.</p>
     *
     * @param authentication the Spring Security authentication of the current request
     * @return a {@link MeResponse} containing basic user data
     */

    @GetMapping("/me")


    public MeResponse me(Authentication authentication) {
        User u = authenticatedUserService.require(authentication);


        return new MeResponse(
                u.getId(),
                u.getUsername(),
                u.getEmail(),
                u.getFirstName(),
                u.getLastName(),
                u.getPhone(),
                u.getRole().name()
        );
    }

    private static UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhone(),
                user.getRole() == null ? null : user.getRole().name(),
                user.getIsActive(),
                user.getEmailVerified(),
                user.getTwoFactorEnabled()
        );
    }


}

