package com.csis231.api.user.service;


import com.csis231.api.common.exception.BadRequestException;
import com.csis231.api.common.exception.ConflictException;
import com.csis231.api.common.exception.ResourceNotFoundException;
import com.csis231.api.user.dto.UserCreateRequest;
import com.csis231.api.user.dto.UserUpdateRequest;
import com.csis231.api.user.model.User;
import com.csis231.api.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service layer for managing {@link User} entities.
 * Handles business rules such as password hashing and unique checks.
 */
@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Retrieves all users without pagination.
     *
     * @return a list containing every {@link User} in the system
     */
    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    /**
     * Retrieves users using pagination.
     *
     * @param pageable paging and sorting information
     * @return a {@link Page} of {@link User} entities
     */
    @Transactional(readOnly = true)
    public Page<User> getUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    /**
     * Looks up a user by identifier.
     *
     * @param id the user ID to search for
     * @return an {@link Optional} containing the user if found
     */
    @Transactional(readOnly = true)
    public Optional<User> getUser(Long id) {
        return userRepository.findById(id);
    }

    /**
     * Creates a new user.  This will throw an IllegalArgumentException
     * if the username or email is already in use.  The password is hashed
     * before the entity is persisted.
     *
     * @param request the user payload to create
     * @return the persisted {@link User}
     * @throws BadRequestException if required fields are missing
     * @throws ConflictException   if username or email already exist
     */
    @Transactional
    public User createUser(UserCreateRequest request) {
        if (request == null || request.username() == null || request.username().isBlank()
                || request.email() == null || request.email().isBlank()
                || request.password() == null || request.password().isBlank()) {
            throw new BadRequestException("Username, email and password are required");
        }
        if (userRepository.existsByUsername(request.username())) {
            throw new ConflictException("Username already in use");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("Email already in use");
        }
        User user = User.builder()
                .username(request.username().trim())
                .email(request.email().trim())
                .password(passwordEncoder.encode(request.password()))
                .firstName(trimToNull(request.firstName()))
                .lastName(trimToNull(request.lastName()))
                .phone(trimToNull(request.phone()))
                .build();
        return userRepository.save(user);
    }

    /**
     * Updates an existing user.  Only fields that are non-null on the
     * {@code updated} object will be applied.  If a new password is provided,
     * it will be hashed before persisting.
     *
     * @param id      the identifier of the user to update
     * @param request the user fields to apply
     * @return an {@link Optional} containing the updated {@link User}
     * @throws BadRequestException if the payload is missing
     * @throws ConflictException   if username or email are taken
     */
    @Transactional
    public Optional<User> updateUser(Long id, UserUpdateRequest request) {
        if (request == null) {
            throw new BadRequestException("User payload is required");
        }
        return userRepository.findById(id).map(existing -> {
            // username change
            if (request.username() != null && !request.username().equals(existing.getUsername())) {
                if (userRepository.existsByUsername(request.username())) {
                    throw new ConflictException("Username already in use");
                }
                existing.setUsername(request.username().trim());
            }
            // email change
            if (request.email() != null && !request.email().equals(existing.getEmail())) {
                if (userRepository.existsByEmail(request.email())) {
                    throw new ConflictException("Email already in use");
                }
                existing.setEmail(request.email().trim());
            }
            // password change
            if (request.password() != null && !request.password().isBlank()) {
                existing.setPassword(passwordEncoder.encode(request.password()));
            }
            // update other properties if present
            if (request.firstName() != null) existing.setFirstName(trimToNull(request.firstName()));
            if (request.lastName() != null) existing.setLastName(trimToNull(request.lastName()));
            if (request.phone() != null) existing.setPhone(trimToNull(request.phone()));
            return userRepository.save(existing);
        });
    }

    /**
     * Deletes a user by identifier.
     *
     * @param id the user ID to remove
     * @return {@code true} when the user existed and was deleted
     * @throws ResourceNotFoundException if the user does not exist
     */
    @Transactional
    public boolean deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User not found: " + id);
        }
        userRepository.deleteById(id);
        return true;
    }

    /**
     * Looks up a user by username.
     *
     * @param username the username to search for
     * @return an {@link Optional} containing the user if found, or empty otherwise
     */

    @Transactional(readOnly = true)
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

}
