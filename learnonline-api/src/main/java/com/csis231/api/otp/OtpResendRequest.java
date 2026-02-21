package com.csis231.api.otp;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request body used when a client asks the server to resend an OTP.
 */

@Data
public class OtpResendRequest {

    /**
     * Username (or e-mail) identifying the account for which the OTP
     * should be resent.
     */

    @NotBlank private String username;

    /**
     * Logical purpose of the OTP. Defaults to {@code "LOGIN_2FA"}.
     */

    private String purpose = "LOGIN_2FA";
}
