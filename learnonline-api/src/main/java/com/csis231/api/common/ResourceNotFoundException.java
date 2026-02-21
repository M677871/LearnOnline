package com.csis231.api.common;

/**
 * Thrown when an expected resource cannot be located.
 */
public class ResourceNotFoundException extends ApplicationException {
    public ResourceNotFoundException(String message) {
        super(message, "NOT_FOUND");
    }
}
