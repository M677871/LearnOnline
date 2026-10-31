package com.csis231.api.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * @param email the account's e-mail address; a username is accepted as well
 */
public record ForgotPasswordRequest(@NotBlank String email) {
}
