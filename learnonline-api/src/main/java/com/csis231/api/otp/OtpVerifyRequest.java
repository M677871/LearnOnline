package com.csis231.api.otp;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body used when a client submits an OTP code for verification.
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OtpVerifyRequest {

    /**
     * Username (or e-mail) identifying the account for which the OTP
     * was issued.
     */

    @NotBlank
    private String username;

    /**
     * The one-time password (OTP) code entered by the user.
     *
     * <p>The validation constraints enforce that the code has exactly six
     * characters.</p>
     */

    @NotBlank
    @Size(min = 6, max = 6)
    private String code;
}
