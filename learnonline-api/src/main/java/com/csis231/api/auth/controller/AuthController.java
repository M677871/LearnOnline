package com.csis231.api.auth.controller;

import com.csis231.api.auth.dto.AuthResponse;
import com.csis231.api.auth.dto.ForgotPasswordRequest;
import com.csis231.api.auth.dto.LoginRequest;
import com.csis231.api.auth.dto.OtpChallengeResponse;
import com.csis231.api.auth.dto.OtpResendRequest;
import com.csis231.api.auth.dto.OtpVerifyRequest;
import com.csis231.api.auth.dto.RegisterRequest;
import com.csis231.api.auth.dto.ResetPasswordRequest;
import com.csis231.api.auth.service.AuthService;
import com.csis231.api.common.dto.MessageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public authentication endpoints. See {@link AuthService} for the login flow.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Answers {@code 202 Accepted} rather than {@code 200}: the login is not complete until
     * the OTP has been verified.
     */
    @PostMapping("/login")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public OtpChallengeResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/otp/verify")
    public AuthResponse verifyOtp(@Valid @RequestBody OtpVerifyRequest request) {
        return authService.verifyOtp(request);
    }

    @PostMapping("/otp/request")
    public MessageResponse requestOtp(@Valid @RequestBody OtpResendRequest request) {
        authService.resendOtp(request.username());
        return new MessageResponse("OTP resent");
    }

    @PostMapping("/register")
    public MessageResponse register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return new MessageResponse("Registered");
    }

    @PostMapping("/password/forgot")
    public MessageResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.requestPasswordReset(request);
        return new MessageResponse("If the email exists, an OTP has been sent");
    }

    @PostMapping("/password/reset")
    public MessageResponse resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return new MessageResponse("Password updated");
    }
}
