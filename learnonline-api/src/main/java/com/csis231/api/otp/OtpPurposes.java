
package com.csis231.api.otp;

/**
 * Collection of well-known OTP purpose codes used in the application.
 *
 * <p>These string constants are typically stored in {@link OtpCode} records and
 * allow you to distinguish between different OTP flows such as login 2FA or
 * password reset.</p>
 */

public final class OtpPurposes {
    private OtpPurposes() {}
    /**
     * Purpose used when sending an OTP as a second factor during login.
     */
    public static final String LOGIN_2FA = "LOGIN_2FA";

    /**
     * Purpose used when sending an OTP that allows the user to reset a password.
     */

    public static final String PASSWORD_RESET = "PASSWORD_RESET";
}
