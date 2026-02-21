package com.csis231.api.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request body used to register a new user in the online learning platform.
 */

@Data
public class RegisterRequest {
    @NotBlank @Size(min = 3, max = 50)
    private String username;

    @NotBlank @Email
    private String email;

    @NotBlank @Size(min = 6)
    private String password;

    private String firstName;
    private String lastName;
    private String phone;

    // optional: STUDENT by default if not provided
    private String role; // "STUDENT" | "INSTRUCTOR" | "ADMIN"
}
