// com.csis231.api.auth.ResetPasswordRequest.java
package com.csis231.api.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body used to complete the "reset password" flow.
 *
 * @param email       the e-mail address associated with the account
 * @param code        the one-time password (OTP) sent for password reset
 * @param newPassword the new password to be stored for the user
 */

public record ResetPasswordRequest(
        @Email @NotBlank String email,
        @NotBlank @Size(min = 6, max = 100) String code,
        @NotBlank @Size(min = 8, max = 100) String newPassword
) {}
