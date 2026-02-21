package com.csis231.api.user;

import com.csis231.api.common.BadRequestException;
import com.csis231.api.common.ConflictException;
import com.csis231.api.common.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
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
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
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
     * @param user the user payload to create
     * @return the persisted {@link User}
     * @throws BadRequestException if required fields are missing
     * @throws ConflictException   if username or email already exist
     */
    @Transactional
    public User createUser(User user) {
        if (user == null || user.getUsername() == null || user.getUsername().isBlank()
                || user.getEmail() == null || user.getEmail().isBlank()
                || user.getPassword() == null || user.getPassword().isBlank()) {
            throw new BadRequestException("Username, email and password are required");
        }
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new ConflictException("Username already in use");
        }
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new ConflictException("Email already in use");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    /**
     * Updates an existing user.  Only fields that are non-null on the
     * {@code updated} object will be applied.  If a new password is provided,
     * it will be hashed before persisting.
     *
     * @param id      the identifier of the user to update
     * @param updated the user fields to apply
     * @return an {@link Optional} containing the updated {@link User}
     * @throws BadRequestException if the payload is missing
     * @throws ConflictException   if username or email are taken
     */
    @Transactional
    public Optional<User> updateUser(Long id, User updated) {
        if (updated == null) {
            throw new BadRequestException("User payload is required");
        }
        return userRepository.findById(id).map(existing -> {
            // username change
            if (updated.getUsername() != null && !updated.getUsername().equals(existing.getUsername())) {
                if (userRepository.existsByUsername(updated.getUsername())) {
                    throw new ConflictException("Username already in use");
                }
                existing.setUsername(updated.getUsername());
            }
            // email change
            if (updated.getEmail() != null && !updated.getEmail().equals(existing.getEmail())) {
                if (userRepository.existsByEmail(updated.getEmail())) {
                    throw new ConflictException("Email already in use");
                }
                existing.setEmail(updated.getEmail());
            }
            // password change
            if (updated.getPassword() != null && !updated.getPassword().isBlank()) {
                existing.setPassword(passwordEncoder.encode(updated.getPassword()));
            }
            // update other properties if present
            if (updated.getFirstName() != null) existing.setFirstName(updated.getFirstName());
            if (updated.getLastName() != null) existing.setLastName(updated.getLastName());
            if (updated.getPhone() != null) existing.setPhone(updated.getPhone());
            if (updated.getIsActive() != null) existing.setIsActive(updated.getIsActive());
            if (updated.getEmailVerified() != null) existing.setEmailVerified(updated.getEmailVerified());
            if (updated.getTwoFactorEnabled() != null) existing.setTwoFactorEnabled(updated.getTwoFactorEnabled());
            if (updated.getRole() != null) existing.setRole(updated.getRole());
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

}
