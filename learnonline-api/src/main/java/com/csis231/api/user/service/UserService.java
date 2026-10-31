package com.csis231.api.user.service;

import com.csis231.api.common.dto.PagedResponse;
import com.csis231.api.common.exception.ConflictException;
import com.csis231.api.user.dto.UserCreateRequest;
import com.csis231.api.user.dto.UserResponse;
import com.csis231.api.user.dto.UserUpdateRequest;
import com.csis231.api.user.mapper.UserMapper;
import com.csis231.api.user.model.User;
import com.csis231.api.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Owns the rules for creating and changing accounts: username and e-mail are unique, values are
 * trimmed and passwords are only ever stored hashed. New accounts always get the default role.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public PagedResponse<UserResponse> list(Pageable pageable) {
        return PagedResponse.fromPage(userRepository.findAll(pageable).map(UserMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public UserResponse get(Long id) {
        return UserMapper.toResponse(userRepository.getOrThrow(id));
    }

    @Transactional
    public UserResponse create(UserCreateRequest request) {
        String username = request.username().trim();
        String email = request.email().trim();
        requireUsernameAvailable(username);
        requireEmailAvailable(email);

        User user = User.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(request.password()))
                .firstName(trimToNull(request.firstName()))
                .lastName(trimToNull(request.lastName()))
                .phone(trimToNull(request.phone()))
                .build();
        return UserMapper.toResponse(userRepository.save(user));
    }

    /**
     * Applies only the fields present in the request. An empty string clears an optional profile
     * field, while {@code null} leaves it untouched.
     */
    @Transactional
    public UserResponse update(Long id, UserUpdateRequest request) {
        User user = userRepository.getOrThrow(id);

        if (request.username() != null) {
            String username = request.username().trim();
            if (!username.equals(user.getUsername())) {
                requireUsernameAvailable(username);
                user.setUsername(username);
            }
        }
        if (request.email() != null) {
            String email = request.email().trim();
            if (!email.equals(user.getEmail())) {
                requireEmailAvailable(email);
                user.setEmail(email);
            }
        }
        if (request.password() != null && !request.password().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.password()));
        }
        if (request.firstName() != null) {
            user.setFirstName(trimToNull(request.firstName()));
        }
        if (request.lastName() != null) {
            user.setLastName(trimToNull(request.lastName()));
        }
        if (request.phone() != null) {
            user.setPhone(trimToNull(request.phone()));
        }
        return UserMapper.toResponse(user);
    }

    @Transactional
    public void delete(Long id) {
        userRepository.delete(userRepository.getOrThrow(id));
    }

    private void requireUsernameAvailable(String username) {
        if (userRepository.existsByUsername(username)) {
            throw new ConflictException("Username already in use");
        }
    }

    private void requireEmailAvailable(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Email already in use");
        }
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
