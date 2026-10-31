package com.csis231.api.auth.dto;

/**
 * Returned once a login is complete: the access token plus the profile data the client displays.
 */
public record AuthResponse(
        String token,
        Long id,
        String username,
        String email,
        String firstName,
        String lastName,
        String role
) {
}
