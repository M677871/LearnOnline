package com.csis231.api.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * @param username the username or e-mail the code was issued for
 */
public record OtpVerifyRequest(
        @NotBlank String username,
        @NotBlank @Size(min = 6, max = 6) String code
) {
}
