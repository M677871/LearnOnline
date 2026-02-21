package com.csis231.api.auth;

import com.csis231.api.common.BadRequestException;
import com.csis231.api.common.UnauthorizedException;
import com.csis231.api.jwt.JwtUtil;
import com.csis231.api.otp.OtpPurposes;
import com.csis231.api.otp.OtpRequiredException;
import com.csis231.api.otp.OtpService;
import com.csis231.api.otp.OtpVerifyRequest;
import com.csis231.api.user.User;
import com.csis231.api.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service that implements the core authentication and
 * password recovery logic for the online learning platform.
 *
 * <p>It delegates credential checking to Spring Security's
 * {@link AuthenticationManager}, handles OTP-based two-factor login
 * via {@link com.csis231.api.otp.OtpService} and issues JWTs via
 * {@link JwtUtil}.</p>
 */

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final OtpService otpService;

    /**
     * Authenticates a user with username and password and optionally
     * triggers an OTP-based second factor.
     *
     * <p>If login requires an OTP, a {@code LOGIN_2FA} OTP is created
     * and sent, and an {@link com.csis231.api.otp.OtpRequiredException}
     * is thrown instead of returning a token. Otherwise an
     * {@link AuthResponse} containing a JWT is returned.</p>
     *
     * @param req the login request with username and password
     * @return an {@link AuthResponse} containing a JWT and basic user data
     *         when OTP is not required
     * @throws BadCredentialsException if the username/password combination is invalid
     * @throws com.csis231.api.otp.OtpRequiredException
     *         if login requires OTP verification
     */

    public AuthResponse login(LoginRequest req) {
        long loginStart = System.currentTimeMillis();
        if (req == null || req.getUsername() == null || req.getUsername().isBlank()
                || req.getPassword() == null || req.getPassword().isBlank()) {
            throw new BadRequestException("Username and password are required");
        }

        long t0 = System.currentTimeMillis();
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword())
        );
        log.info("[TIMING] AuthenticationManager.authenticate() took {} ms",
                System.currentTimeMillis() - t0);

        if (auth == null || !auth.isAuthenticated()) {
            throw new BadCredentialsException("Invalid username or password");
        }

        t0 = System.currentTimeMillis();
        User user = userRepository.findByUsername(req.getUsername())
                .orElseGet(() -> userRepository.findByEmail(req.getUsername())
                        .orElseThrow(() -> new BadCredentialsException("Invalid username or password")));
        log.info("[TIMING] User lookup took {} ms", System.currentTimeMillis() - t0);

        // Always require OTP for login (or gate by a flag user.isTwoFactorEnabled())
        boolean requiresLoginOtp = true;
        if (requiresLoginOtp) {
            t0 = System.currentTimeMillis();
            otpService.createAndSend(user, OtpPurposes.LOGIN_2FA);
            log.info("[TIMING] otpService.createAndSend() took {} ms", System.currentTimeMillis() - t0);
            log.info("[TIMING] AuthService.login() total took {} ms",
                    System.currentTimeMillis() - loginStart);
            // Important: do not return a JWT here.
            throw new OtpRequiredException(user.getUsername());
        }

        // If you ever disable OTP for some users:
        String token = jwtUtil.generateToken(user.getUsername());
        return new AuthResponse(
                token,
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole().name()
        );
    }

    /**
     * Verifies a LOGIN_2FA OTP code and issues a JWT on success.
     *
     * @param req the OTP verification request containing username or e-mail
     *            and the OTP code
     * @return an {@link AuthResponse} containing the JWT and user data
     * @throws BadCredentialsException if the username/e-mail or code are invalid
     */

    public AuthResponse verifyOtp(OtpVerifyRequest req) {
        if (req == null || req.getUsername() == null || req.getUsername().isBlank()
                || req.getCode() == null || req.getCode().isBlank()) {
            throw new BadRequestException("Username and OTP code are required");
        }

        String id = req.getUsername();
        String code = req.getCode();

        User user = userRepository.findByUsername(id)
                .orElseGet(() -> userRepository.findByEmail(id)
                        .orElseThrow(() -> new BadCredentialsException("Invalid email or code")));

        otpService.verifyOtpOrThrow(user, OtpPurposes.LOGIN_2FA, code);

        String token = jwtUtil.generateToken(user.getUsername());
        return new AuthResponse(
                token,
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole().name()
        );
    }

    /**
     * Resends a login OTP for the given username or e-mail.
     *
     * <p>If the identifier is blank or does not resolve to any user,
     * this method returns silently.</p>
     *
     * @param username the username or e-mail identifying the user
     */

    public void resendOtp(String username) {
        if (username == null || username.isBlank()) {
            throw new BadRequestException("Username is required to resend OTP");
        }
        userRepository.findByUsername(username)
                .or(() -> userRepository.findByEmail(username))
                .ifPresent(u -> otpService.createAndSend(u, OtpPurposes.LOGIN_2FA));
    }

    /**
     * Starts a password-reset flow by issuing a PASSWORD_RESET OTP.
     *
     * @param req request carrying the e-mail (or username, according to the
     *            implemented logic)
     * @throws BadCredentialsException if no matching user is found
     */

    public void requestPasswordReset(ForgotPasswordRequest req) {
        if (req == null || req.email() == null || req.email().isBlank()) {
            throw new BadRequestException("Email is required");
        }

        User user = userRepository.findByEmail(req.email())
                .or(() -> userRepository.findByUsername(req.email()))
                .orElseThrow(() -> new UnauthorizedException("Unknown user"));

        otpService.createAndSend(user, OtpPurposes.PASSWORD_RESET);
    }

    /**
     * Resets the user's password after successful PASSWORD_RESET OTP
     * verification.
     *
     * @param req request containing the user's e-mail, the OTP code and
     *            the new password
     * @throws BadCredentialsException if the e-mail is unknown or the OTP is invalid
     */

    @Transactional
    public void resetPassword(ResetPasswordRequest req) {
        if (req == null || req.email() == null || req.email().isBlank()
                || req.code() == null || req.code().isBlank()
                || req.newPassword() == null || req.newPassword().isBlank()) {
            throw new BadRequestException("Email, OTP code and new password are required");
        }
        User user = userRepository.findByEmail(req.email())
                .orElseThrow(() -> new UnauthorizedException("Unknown email"));
        otpService.verifyOtpOrThrow(user, OtpPurposes.PASSWORD_RESET, req.code());
        user.setPassword(passwordEncoder.encode(req.newPassword()));
    }
}
