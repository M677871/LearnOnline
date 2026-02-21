package com.csis231.api.otp;

import lombok.*;

/**
 * Runtime exception indicating that an OTP is required or that the supplied
 * OTP is invalid for the given user.
 *
 * <p>Typical usage is in the OTP validation layer: if the OTP is missing,
 * expired or does not match, this exception is thrown and translated into an
 * appropriate HTTP response by a controller or global exception handler.</p>
 */

@Getter
@Setter
@ToString
public class OtpRequiredException extends RuntimeException {

    /**
     * Username (or login identifier) for which the OTP is required.
     */

    private final String username;

    /**
     * Creates a new {@code OtpRequiredException} for the given username.
     *
     * @param username the user for whom an OTP is required
     */

    public OtpRequiredException(String username) {
        super("OTP required for user: " + username);
        this.username = username;
    }

}
