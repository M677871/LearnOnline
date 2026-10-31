package com.csis231.api.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MeResponse(
        Long id,
        String username,
        String email,
        String firstName,
        String lastName,
        String phone,
        String role
) {
    /**
     * Display name for the client, falling back to the username when no name is set.
     * Jackson only serializes record components by default, hence the explicit {@link JsonProperty}.
     */
    @JsonProperty
    public String fullName() {
        String first = firstName == null ? "" : firstName.trim();
        String last = lastName == null ? "" : lastName.trim();
        String full = (first + " " + last).trim();
        return full.isEmpty() ? username : full;
    }
}
