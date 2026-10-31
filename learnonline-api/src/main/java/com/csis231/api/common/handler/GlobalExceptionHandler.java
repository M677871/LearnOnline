package com.csis231.api.common.handler;

import com.csis231.api.common.dto.ErrorResponse;
import com.csis231.api.common.exception.ApplicationException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.Instant;
import java.util.List;

/**
 * Translates exceptions raised in the MVC layer into {@link ErrorResponse} payloads.
 *
 * <p>The base class maps Spring's own exceptions (malformed JSON, unknown routes, unsupported methods,
 * type mismatches, ...) to their proper status, and {@link #handleExceptionInternal} gives them the
 * same payload as our errors. Failures inside the security filter chain never get here.</p>
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<ErrorResponse> handleApplication(ApplicationException ex, WebRequest request) {
        return ResponseEntity.status(ex.getStatus()).body(build(ex.getMessage(), ex.getCode(), request));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException ex, WebRequest request) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(build(ex.getMessage(), "UNAUTHORIZED", request));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, WebRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(build("Access denied", "FORBIDDEN", request));
    }

    /**
     * Covers deletes of rows that are still referenced, and uniqueness races that slip past the
     * existence checks in the services. The database message is logged but never returned.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex, WebRequest request) {
        log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(build("The operation conflicts with existing data", "CONFLICT", request));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex, WebRequest request) {
        String message = ex.getConstraintViolations().stream()
                .findFirst()
                .map(violation -> violation.getPropertyPath() + " " + violation.getMessage())
                .orElse("Validation failed");
        return ResponseEntity.badRequest().body(build(message, "VALIDATION_FAILED", request));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, WebRequest request) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.internalServerError().body(build("Internal server error", "INTERNAL_ERROR", request));
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        return validationFailure(ex.getBindingResult().getFieldErrors(), request);
    }

    /**
     * Raised instead of {@link MethodArgumentNotValidException} when the validated body is a collection,
     * such as the list of questions accepted by the quiz API.
     */
    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex,
                                                                            HttpHeaders headers,
                                                                            HttpStatusCode status,
                                                                            WebRequest request) {
        return validationFailure(ex.getAllErrors(), request);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex,
                                                             Object body,
                                                             HttpHeaders headers,
                                                             HttpStatusCode statusCode,
                                                             WebRequest request) {
        if (statusCode.is5xxServerError()) {
            log.error("Request failed", ex);
        }
        HttpStatus status = HttpStatus.valueOf(statusCode.value());
        return new ResponseEntity<>(build(status.getReasonPhrase(), status.name(), request), headers, statusCode);
    }

    private ResponseEntity<Object> validationFailure(List<? extends MessageSourceResolvable> errors, WebRequest request) {
        String message = errors.stream()
                .findFirst()
                .map(GlobalExceptionHandler::describe)
                .orElse("Validation failed");
        return ResponseEntity.badRequest().body(build(message, "VALIDATION_FAILED", request));
    }

    private static String describe(MessageSourceResolvable error) {
        return error instanceof FieldError field
                ? field.getField() + " " + field.getDefaultMessage()
                : error.getDefaultMessage();
    }

    private static ErrorResponse build(String message, String code, WebRequest request) {
        String path = request instanceof ServletWebRequest servletRequest
                ? servletRequest.getRequest().getRequestURI()
                : null;
        return new ErrorResponse(message, code, Instant.now(), path);
    }
}
