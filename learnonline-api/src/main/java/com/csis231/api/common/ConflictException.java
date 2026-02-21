package com.csis231.api.common;

/**
 * Thrown when a request violates a uniqueness or state constraint.
 */
public class ConflictException extends ApplicationException {
    public ConflictException(String message) {
        super(message, "CONFLICT");
    }
}
