package com.csis231.api.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when the caller may not perform an action. The {@code UNAUTHORIZED} error code predates
 * this class and is kept because it is part of the API contract.
 */
public class ForbiddenException extends ApplicationException {

    public ForbiddenException(String message) {
        super(message, HttpStatus.FORBIDDEN, "UNAUTHORIZED");
    }
}
