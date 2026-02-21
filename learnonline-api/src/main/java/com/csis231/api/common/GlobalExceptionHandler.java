package com.csis231.api.common;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import java.time.Instant;

/**
 * Centralizes translation of exceptions into consistent HTTP responses.
 */
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Builds a standard {@link ErrorResponse} from message/code and request path.
     *
     * @param message human-readable error description
     * @param code    machine-readable error code
     * @param request the current request context
     * @return a populated {@link ErrorResponse}
     */
    private ErrorResponse build(String message, String code, WebRequest request) {
        String path = null;
        if (request instanceof ServletWebRequest servletRequest) {
            path = servletRequest.getRequest().getRequestURI();
        }
        return ErrorResponse.builder()
                .message(message)
                .code(code)
                .timestamp(Instant.now())
                .path(path)
                .build();
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(BadRequestException ex, WebRequest request) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(build(ex.getMessage(), ex.getCode(), request));
    }

    /**
     * Handles unauthorized/forbidden errors raised by the application.
     *
     * @param ex      the unauthorized exception
     * @param request the current request
     * @return a 403 response with standardized error payload
     */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(UnauthorizedException ex, WebRequest request) {
        // Use 403 as the default for unauthorized/forbidden resource access
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(build(ex.getMessage(), ex.getCode(), request));
    }

    /**
     * Handles Spring Security access denied exceptions.
     *
     * @param ex      the access denied exception
     * @param request the current request
     * @return a 403 response with standardized error payload
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, WebRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(build("Access denied", "FORBIDDEN", request));
    }

    /**
     * Handles bad credential errors.
     *
     * @param ex      bad credentials exception
     * @param request the current request
     * @return a 401 response with standardized error payload
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex, WebRequest request) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(build(ex.getMessage(), "UNAUTHORIZED", request));
    }

    /**
     * Handles not-found exceptions raised by the application.
     *
     * @param ex      resource not found exception
     * @param request the current request
     * @return a 404 response with standardized error payload
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex, WebRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(build(ex.getMessage(), ex.getCode(), request));
    }

    /**
     * Handles conflict exceptions raised by the application.
     *
     * @param ex      conflict exception
     * @param request the current request
     * @return a 409 response with standardized error payload
     */
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(ConflictException ex, WebRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(build(ex.getMessage(), ex.getCode(), request));
    }

    /**
     * Handles bean validation errors from @Valid annotated payloads.
     *
     * @param ex      validation exception with binding errors
     * @param request the current request
     * @return a 400 response with the first validation error message
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, WebRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(err -> err.getField() + " " + err.getDefaultMessage())
                .orElse("Validation failed");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(build(message, "VALIDATION_FAILED", request));
    }

    /**
     * Handles constraint violations raised outside of controller binding.
     *
     * @param ex      constraint violation exception
     * @param request the current request
     * @return a 400 response with the first violation message
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex, WebRequest request) {
        String message = ex.getConstraintViolations().stream()
                .findFirst()
                .map(v -> v.getPropertyPath() + " " + v.getMessage())
                .orElse("Validation failed");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(build(message, "VALIDATION_FAILED", request));
    }

    /**
     * Fallback handler for unhandled exceptions.
     *
     * @param ex      the unexpected exception
     * @param request the current request
     * @return a 500 response with a generic error message
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, WebRequest request) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(build("Internal server error", "INTERNAL_ERROR", request));
    }
}
