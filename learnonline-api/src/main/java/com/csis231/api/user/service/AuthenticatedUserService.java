package com.csis231.api.user.service;

import com.csis231.api.common.exception.ForbiddenException;
import com.csis231.api.common.exception.ResourceNotFoundException;
import com.csis231.api.user.model.User;
import com.csis231.api.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resolves the {@link User} behind the current request. Controllers receive a {@code null}
 * {@link Authentication} for anonymous callers, which public routes such as the course detail must tolerate.
 */
@Service
@RequiredArgsConstructor
public class AuthenticatedUserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public User require(Authentication authentication) {
        if (authentication == null) {
            throw new ForbiddenException("Authentication required");
        }
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + authentication.getName()));
    }

    @Transactional(readOnly = true)
    public User optional(Authentication authentication) {
        if (authentication == null) {
            return null;
        }
        return userRepository.findByUsername(authentication.getName()).orElse(null);
    }
}
