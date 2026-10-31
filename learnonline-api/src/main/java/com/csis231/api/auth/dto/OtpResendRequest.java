package com.csis231.api.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * @param username the username or e-mail to send a new login code to
 */
public record OtpResendRequest(@NotBlank String username) {
}
