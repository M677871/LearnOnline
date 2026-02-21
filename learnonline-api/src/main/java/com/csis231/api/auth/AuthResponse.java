// AuthResponse.java
package com.csis231.api.auth;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Response returned after successful authentication or OTP verification.
 *
 * <p>Contains the issued JWT together with basic user profile data that
 * the frontend of the online learning platform needs.</p>
 */
@Data
@AllArgsConstructor
public class AuthResponse {

    /** The JSON Web Token (JWT) issued for the authenticated user. */
    private String token;

    /** Database identifier of the authenticated user. */
    private Long id;

    /** Unique username of the authenticated user. */
    private String username;

    /** E-mail address of the authenticated user. */
    private String email;

    /** First name of the user. */
    private String firstName;

    /** Last name of the user. */
    private String lastName;

    /** Application role of the user (for example STUDENT, INSTRUCTOR, ADMIN). */
    private String role;
}
