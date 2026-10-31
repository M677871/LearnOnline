package com.csis231.api.user.mapper;

import com.csis231.api.user.dto.MeResponse;
import com.csis231.api.user.dto.UserResponse;
import com.csis231.api.user.model.User;

public final class UserMapper {

    private UserMapper() {
    }

    public static UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhone(),
                user.getRole().name(),
                user.getIsActive(),
                user.getEmailVerified(),
                user.getTwoFactorEnabled()
        );
    }

    public static MeResponse toMeResponse(User user) {
        return new MeResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhone(),
                user.getRole().name()
        );
    }
}
