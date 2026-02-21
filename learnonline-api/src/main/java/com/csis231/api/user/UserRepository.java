package com.csis231.api.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link User} entities.
 *
 * <p>Provides helpers for looking up users by username or e-mail and for
 * checking uniqueness constraints.</p>
 */

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Finds a user by username.
     *
     * @param username the username to search for
     * @return an {@link Optional} containing the user if found, or empty otherwise
     */

    Optional<User> findByUsername(String username);

    /**
     * Finds a user by e-mail address.
     *
     * @param email the e-mail address to search for
     * @return an {@link Optional} containing the user if found, or empty otherwise
     */

    Optional<User> findByEmail(String email);

    /**
     * Checks whether a user with the given username already exists.
     *
     * @param username the username to check
     * @return {@code true} if a user with this username exists; {@code false} otherwise
     */

    boolean existsByUsername(String username);

    /**
     * Checks whether a user with the given e-mail address already exists.
     *
     * @param email the e-mail address to check
     * @return {@code true} if a user with this e-mail exists; {@code false} otherwise
     */

    boolean existsByEmail(String email);
}
