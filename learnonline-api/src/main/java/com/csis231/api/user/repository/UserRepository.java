package com.csis231.api.user.repository;

import com.csis231.api.common.exception.ResourceNotFoundException;
import com.csis231.api.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    default User getOrThrow(Long id) {
        return findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }
}
