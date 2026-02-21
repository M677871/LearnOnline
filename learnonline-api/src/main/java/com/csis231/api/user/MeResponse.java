
package com.csis231.api.user;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * DTO returned by the {@code /me} endpoint containing basic profile
 * information about the currently authenticated user.
 *
 * <p>This object is meant to be lightweight and safe to expose to the
 * frontend of the online learning platform.</p>
 */

@Data
@AllArgsConstructor
public class MeResponse {
    private Long   id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private String role;

    /**
     * Returns a human-friendly display name for the user.
     *
     * <p>If both first name and last name are blank or {@code null},
     * the username is returned instead.</p>
     *
     * @return full name if available; otherwise the username
     */

    public String getFullName() {
        String fn = (firstName == null ? "" : firstName.trim());
        String ln = (lastName  == null ? "" : lastName.trim());
        String full = (fn + " " + ln).trim();
        return full.isEmpty() ? username : full;
    }
}
