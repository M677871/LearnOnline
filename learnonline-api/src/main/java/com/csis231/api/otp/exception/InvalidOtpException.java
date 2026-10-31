package com.csis231.api.otp.exception;

import com.csis231.api.common.exception.ApplicationException;
import org.springframework.http.HttpStatus;

/**
 * Thrown for any failed verification. Callers get the same response for a wrong, expired or used
 * code and for an unknown account, so a failed attempt reveals nothing about which one it was.
 */
public class InvalidOtpException extends ApplicationException {

    public InvalidOtpException(String message) {
        super(message, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED");
    }

    public InvalidOtpException() {
        this("Invalid email or code");
    }
}
