
package com.csis231.api.auth;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request body used for username/password authentication.
 */

@Data
public class LoginRequest {
    @NotBlank private String username;
    @NotBlank private String password;
}
