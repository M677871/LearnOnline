package com.csis231.api.common.dto;

import java.time.Instant;

/**
 * Error body returned by every failed request that reaches the MVC layer.
 *
 * @param code machine-readable error code that clients can switch on
 * @param path URI of the request that failed
 */
public record ErrorResponse(String message, String code, Instant timestamp, String path) {
}
