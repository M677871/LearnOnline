package com.csis231.api.user.dto;

public record UserResponse(
        Long id,
        String username,
        String email,
        String firstName,
        String lastName,
        String phone,
        String role,
        Boolean active,
        Boolean emailVerified,
        Boolean twoFactorEnabled
) {
}
