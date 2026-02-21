package com.csis231.api.otp;

import com.csis231.api.auth.AuthResponse;
import com.csis231.api.auth.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller that exposes endpoints for requesting and verifying OTP
 * codes during the authentication flow of the online learning platform.
 *
 * <p>The controller delegates the core logic to {@link AuthService} and
 * translates OTP-related errors into appropriate HTTP responses.</p>
 */

@RestController
@RequestMapping("/api/auth/otp")
@RequiredArgsConstructor
@Slf4j
public class OtpController {

    private final AuthService authService;

    /**
     * Verifies an OTP code submitted by the client and, if valid, completes
     * the authentication flow (for example by issuing JWT tokens).
     *
     * @param req request payload containing the username and OTP code
     * @return {@code 200 OK} with an {@link AuthResponse} on success, or
     *         {@code 401 Unauthorized} when the OTP is invalid or expired
     */

    @PostMapping("/verify")
    public ResponseEntity<?> verify(@Valid @RequestBody OtpVerifyRequest req) {
        try {
            AuthResponse resp = authService.verifyOtp(req); // issues JWT on success
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            log.warn("OTP verify failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid email or code"));
        }
    }

    /**
     * Resends an OTP for the given user.
     *
     * <p>This endpoint is typically called when the user did not receive the
     * original OTP or it expired before they could use it.</p>
     *
     * @param req request payload containing the username (and optionally a purpose)
     * @return {@code 200 OK} with a simple confirmation message when the OTP
     *         resend operation has been triggered
     */

    @PostMapping("/request")
    public ResponseEntity<?> request(@Valid @RequestBody OtpResendRequest req) {
        authService.resendOtp(req.getUsername());
        return ResponseEntity.ok(Map.of("message", "OTP resent"));
    }
}
