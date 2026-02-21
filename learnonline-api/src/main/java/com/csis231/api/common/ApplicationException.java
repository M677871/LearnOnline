package com.csis231.api.common;

import lombok.Getter;

/**
 * Base runtime exception for domain-level errors.
 */
@Getter
public class ApplicationException extends RuntimeException {
    private final String code;

    public ApplicationException(String message) {
        super(message);
        this.code = null;
    }

    /**
     * Creates a new application exception with a message and explicit error code.
     *
     * @param message human-readable description of the error
     * @param code    machine-readable error code
     */
    public ApplicationException(String message, String code) {
        super(message);
        this.code = code;
    }
}
