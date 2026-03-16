package com.csis231.api.common.exception;

/**
 * Thrown when a request lacks the proper authorization.
 */
public class UnauthorizedException extends ApplicationException {
    public UnauthorizedException(String message) {
        super(message, "UNAUTHORIZED");
    }
}
