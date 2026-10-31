package com.csis231.api.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Base class for expected business failures. Each subclass fixes its HTTP status and error code,
 * which lets {@code GlobalExceptionHandler} translate all of them with a single handler.
 */
@Getter
public abstract class ApplicationException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    protected ApplicationException(String message, HttpStatus status, String code) {
        super(message);
        this.status = status;
        this.code = code;
    }
}
