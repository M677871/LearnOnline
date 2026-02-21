package com.example.demo.auth;

import com.example.demo.common.ApiClient;
import com.example.demo.common.ApiResponse;
import com.example.demo.common.TokenStore;
import com.example.demo.model.AuthResponse;
import com.example.demo.model.LoginRequest;
import com.example.demo.model.MeResponse;
import com.example.demo.model.OtpVerifyRequest;
import com.example.demo.model.RegisterRequest;
import com.example.demo.model.ResetPasswordRequest;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.Map;

/**
 * Authentication API wrapper used by the JavaFX client.
 */
public class AuthApi {
    private final ApiClient client = new ApiClient();

    /**
     * Attempts username/password login, returning an OTP challenge when required.
     *
     * @param req the login payload
     * @return an {@link AuthResponse}; if OTP is required, {@code otpRequired} is true and token is null
     */
    public AuthResponse login(LoginRequest req) {
        ApiResponse<String> response = client.post("/api/auth/login", req);
        if (response.getStatusCode() == 202) {
            Map<String, Object> body = client.read(response.getRawBody(), new TypeReference<Map<String, Object>>() {});
            boolean otpRequired = body != null && Boolean.TRUE.equals(body.get("otpRequired"));
            if (otpRequired && body != null && body.get("username") instanceof String user) {
                TempAuth.username = user;
            }
            AuthResponse authResponse = new AuthResponse();
            authResponse.setOtpRequired(true);
            return authResponse;
        }

        AuthResponse auth = client.read(response.getRawBody(), new TypeReference<AuthResponse>() {});
        if (auth != null && auth.getToken() != null && !auth.getToken().isBlank()) {
            TokenStore.set(auth.getToken());
        }
        return auth;
    }

    /**
     * Requests that an OTP be sent to the given username or email.
     *
     * @param username the username/email to receive the OTP
     */
    public void requestOtp(String username) {
        client.post("/api/auth/otp/request", Map.of("username", username));
    }

    /**
     * Verifies an OTP and stores the issued JWT on success.
     *
     * @param req the OTP verification payload
     * @return the authenticated {@link AuthResponse}
     */
    public AuthResponse verifyOtp(OtpVerifyRequest req) {
        ApiResponse<AuthResponse> resp = client.post("/api/auth/otp/verify", req, new TypeReference<AuthResponse>() {});
        AuthResponse body = resp.getBody();
        if (body != null && body.getToken() != null && !body.getToken().isBlank()) {
            TokenStore.set(body.getToken());
        }
        return body;
    }

    /**
     * Registers a new user account.
     *
     * @param req the registration request
     */
    public void register(RegisterRequest req) {
        client.post("/api/auth/register", req);
    }

    /**
     * Requests a password reset OTP to be sent to the specified email.
     *
     * @param email the target email
     */
    public void requestResetOtp(String email) {
        client.post("/api/auth/password/forgot", Map.of("email", email));
    }

    /**
     * Resets a password using an OTP code.
     *
     * @param req the reset-password payload
     */
    public void resetWithCode(ResetPasswordRequest req) {
        client.post("/api/auth/password/reset", req);
    }

    /**
     * Fetches the current authenticated user's profile.
     *
     * @return a {@link MeResponse} describing the user
     */
    public MeResponse me() {
        return client.get("/api/csis-users/me", new TypeReference<MeResponse>() {}).getBody();
    }
}
