package com.csis231.api.common;

/**
 * Thrown when a request cannot be processed due to invalid input.
 */
public class BadRequestException extends ApplicationException {
    public BadRequestException(String message) {
        super(message, "BAD_REQUEST");
    }
}
