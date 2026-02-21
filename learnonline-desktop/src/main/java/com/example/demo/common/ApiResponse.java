package com.example.demo.common;

/**
 * Wrapper around an HTTP response with both parsed and raw payloads.
 */
public class ApiResponse<T> {
    private final int statusCode;
    private final T body;
    private final String rawBody;

    public ApiResponse(int statusCode, T body, String rawBody) {
        this.statusCode = statusCode;
        this.body = body;
        this.rawBody = rawBody;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public T getBody() {
        return body;
    }

    public String getRawBody() {
        return rawBody;
    }
}
