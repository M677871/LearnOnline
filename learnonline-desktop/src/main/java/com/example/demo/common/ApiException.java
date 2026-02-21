package com.example.demo.common;

/**
 * Client-side exception representing an HTTP error response.
 */
public class ApiException extends RuntimeException {
    private final int statusCode;
    private final String errorCode;

    public ApiException(int statusCode, String message, String errorCode) {
        super(message);
        this.statusCode = statusCode;
        this.errorCode = errorCode;
    }

    public ApiException(int statusCode, String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
        this.errorCode = errorCode;
    }

    /**
     * Returns the HTTP status code returned by the backend.
     *
     * @return the numeric HTTP status code
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * Returns the backend-specific error code, if provided.
     *
     * @return a machine-readable error code or {@code null}
     */
    public String getErrorCode() {
        return errorCode;
    }
}
