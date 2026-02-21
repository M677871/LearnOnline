package com.example.demo.auth;

/**
 * Small in-memory holder for temporary authentication state.
 *
 * <p>This class is used by the desktop client to pass values between
 * screens during short-lived flows such as OTP verification and password
 * reset. The fields are {@code public static} on purpose and are cleared
 * or overwritten as the flow progresses.</p>
 */
public final class TempAuth {

    /**
     * Username of the user currently going through the login + OTP flow.
     *
     * <p>Typically set by the login screen when the backend indicates that
     * OTP is required, and then used by the OTP screen to know which
     * account to verify the code against.</p>
     */
    public static String username;

    /**
     * E-mail address for which a password-reset OTP was requested.
     *
     * <p>Typically set by the "forgot password" screen so that the reset
     * screen can prefill the e-mail field for convenience.</p>
     */
    public static String resetEmail;

    /**
     * Utility class; not meant to be instantiated.
     */
    private TempAuth() {}
}
