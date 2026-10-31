package com.csis231.api.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @Email @NotBlank String email,
        @NotBlank @Size(min = 6, max = 100) String code,
        @NotBlank @Size(min = 8, max = 100) String newPassword
) {
}
