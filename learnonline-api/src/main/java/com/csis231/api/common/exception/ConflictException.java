package com.csis231.api.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a request violates a uniqueness or state rule.
 */
public class ConflictException extends ApplicationException {

    public ConflictException(String message) {
        super(message, HttpStatus.CONFLICT, "CONFLICT");
    }
}
