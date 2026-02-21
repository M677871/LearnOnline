package com.example.demo.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request body used by the client to complete the password-reset flow.
 *
 * @param email       e-mail for which the reset OTP was requested
 * @param code        the password-reset code (OTP) received by the user
 * @param newPassword the new password that should be stored for this account
 *
 * <p>Submitted after the user receives an OTP via e-mail as part of the
 * forgot-password sequence.</p>
 */
public record ResetPasswordRequest(
        @JsonProperty("email") String email,
        @JsonProperty("code") String code,
        String newPassword
) {}
