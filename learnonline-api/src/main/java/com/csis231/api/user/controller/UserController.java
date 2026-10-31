package com.csis231.api.user.controller;

import com.csis231.api.common.dto.PagedResponse;
import com.csis231.api.common.util.Paging;
import com.csis231.api.user.dto.MeResponse;
import com.csis231.api.user.dto.UserCreateRequest;
import com.csis231.api.user.dto.UserResponse;
import com.csis231.api.user.dto.UserUpdateRequest;
import com.csis231.api.user.mapper.UserMapper;
import com.csis231.api.user.service.AuthenticatedUserService;
import com.csis231.api.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Administrator user management, plus {@code /me} for any signed-in user.
 * The URL rules in {@code SecurityConfig} restrict everything except {@code /me} to admins.
 */
@RestController
@RequestMapping("/api/csis-users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final AuthenticatedUserService authenticatedUserService;

    @GetMapping
    public PagedResponse<UserResponse> list(@RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "10") int size) {
        return userService.list(Paging.of(page, size));
    }

    @GetMapping("/{id}")
    public UserResponse get(@PathVariable Long id) {
        return userService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@Valid @RequestBody UserCreateRequest request) {
        return userService.create(request);
    }

    @PutMapping("/{id}")
    public UserResponse update(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request) {
        return userService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        userService.delete(id);
    }

    @GetMapping("/me")
    public MeResponse me(Authentication authentication) {
        return UserMapper.toMeResponse(authenticatedUserService.require(authentication));
    }
}
