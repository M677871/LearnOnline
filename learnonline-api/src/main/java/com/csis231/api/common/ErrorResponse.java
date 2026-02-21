package com.csis231.api.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Standard error payload returned by the API.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    private String message;
    private String code;
    private Instant timestamp;
    private String path;

    /**
     * Builds an {@link ErrorResponse} with the given values.
     *
     * @param message   human-readable error description
     * @param code      machine-readable error code
     * @param timestamp time the error was generated
     * @param path      request path that produced the error
     */
}
