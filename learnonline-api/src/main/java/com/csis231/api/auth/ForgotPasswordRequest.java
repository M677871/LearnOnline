
package com.csis231.api.auth;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body used to start the "forgot password" flow.
 *
 * @param email the e-mail address associated with the account
 */

public record ForgotPasswordRequest(
         @NotBlank String email
) {}
