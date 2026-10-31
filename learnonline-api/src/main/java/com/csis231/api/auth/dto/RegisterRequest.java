package com.csis231.api.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Self-registration data. There is deliberately no role field: anyone can call this endpoint,
 * so accepting a role would let a caller make themselves an administrator.
 */
public record RegisterRequest(
        @NotBlank @Size(min = 3, max = 50) String username,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 6) String password,
        String firstName,
        String lastName,
        String phone
) {
}
