package com.csis231.api.auth.service;

import com.csis231.api.auth.dto.AuthResponse;
import com.csis231.api.auth.dto.ForgotPasswordRequest;
import com.csis231.api.auth.dto.LoginRequest;
import com.csis231.api.auth.dto.OtpChallengeResponse;
import com.csis231.api.auth.dto.OtpVerifyRequest;
import com.csis231.api.auth.dto.RegisterRequest;
import com.csis231.api.auth.dto.ResetPasswordRequest;
import com.csis231.api.otp.exception.InvalidOtpException;
import com.csis231.api.otp.model.OtpPurpose;
import com.csis231.api.otp.service.OtpService;
import com.csis231.api.security.JwtService;
import com.csis231.api.user.dto.UserCreateRequest;
import com.csis231.api.user.model.User;
import com.csis231.api.user.repository.UserRepository;
import com.csis231.api.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Registration, two-step login and password recovery.
 *
 * <p>Login always has two steps: {@link #login} checks the password and e-mails a code, then
 * {@link #verifyOtp} redeems the code and issues the JWT. A password alone never yields a token.
 * Logging out is client-side, by discarding the token.</p>
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final OtpService otpService;

    public void register(RegisterRequest req) {
        userService.create(new UserCreateRequest(
                req.username(), req.email(), req.password(), req.firstName(), req.lastName(), req.phone()));
    }

    /**
     * Checks the password and starts the second login step.
     *
     * @throws org.springframework.security.core.AuthenticationException if the credentials are
     *         wrong or the account is disabled
     */
    public OtpChallengeResponse login(LoginRequest req) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.username(), req.password()));

        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));

        otpService.createAndSend(user, OtpPurpose.LOGIN_2FA);
        return new OtpChallengeResponse(true, OtpPurpose.LOGIN_2FA, user.getUsername());
    }

    public AuthResponse verifyOtp(OtpVerifyRequest req) {
        User user = findByUsernameOrEmail(req.username()).orElseThrow(InvalidOtpException::new);
        otpService.verifyOrThrow(user, OtpPurpose.LOGIN_2FA, req.code());
        return new AuthResponse(
                jwtService.generateToken(user.getUsername()),
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole().name()
        );
    }

    /**
     * Sends a new login code. Unknown identifiers are ignored so that the response does not reveal
     * which accounts exist.
     */
    public void resendOtp(String usernameOrEmail) {
        findByUsernameOrEmail(usernameOrEmail)
                .ifPresent(user -> otpService.createAndSend(user, OtpPurpose.LOGIN_2FA));
    }

    /**
     * Sends a reset code. Unknown identifiers are ignored so that the response does not reveal
     * which accounts exist.
     */
    public void requestPasswordReset(ForgotPasswordRequest req) {
        userRepository.findByEmail(req.email())
                .or(() -> userRepository.findByUsername(req.email()))
                .ifPresent(user -> otpService.createAndSend(user, OtpPurpose.PASSWORD_RESET));
    }

    /**
     * Redeeming the code and storing the new password share one transaction, so neither can
     * happen without the other.
     */
    @Transactional
    public void resetPassword(ResetPasswordRequest req) {
        User user = userRepository.findByEmail(req.email()).orElseThrow(InvalidOtpException::new);
        otpService.verifyOrThrow(user, OtpPurpose.PASSWORD_RESET, req.code());
        user.setPassword(passwordEncoder.encode(req.newPassword()));
    }

    private Optional<User> findByUsernameOrEmail(String identifier) {
        return userRepository.findByUsername(identifier)
                .or(() -> userRepository.findByEmail(identifier));
    }
}
