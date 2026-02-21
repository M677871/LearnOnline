package com.csis231.api.otp;

import com.csis231.api.common.BadRequestException;
import com.csis231.api.common.UnauthorizedException;
import com.csis231.api.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Domain service responsible for generating, storing and validating
 * one-time passwords (OTP) for different authentication flows in the
 * online learning platform.
 *
 * <p>This service coordinates the persistence of {@link OtpCode} entities
 * and the delivery of codes via mail, and encapsulates all rules related to
 * OTP creation, expiration, reuse and invalidation.</p>
 */

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private final OtpCodeRepository repo;
    private final JavaMailSender mailSender;

    @Value("${mail.from:}")
    private String from;


    // Default variant (keeps your existing login OTP flow working)
    /**
     * Creates and sends an OTP for the given user and purpose using default settings.
     *
     * @param user    the user who will receive the OTP
     * @param purpose the OTP purpose (e.g., LOGIN_2FA or PASSWORD_RESET)
     * @return the generated OTP code
     * @throws BadRequestException if user or purpose are missing
     */
    @Transactional
    public String createAndSend(User user, String purpose) {
        if (user == null) {
            throw new BadRequestException("User is required to create an OTP");
        }
        if (purpose == null || purpose.isBlank()) {
            throw new BadRequestException("Purpose is required to create an OTP");
        }
        if (purpose.equals(OtpPurposes.LOGIN_2FA))
             return createAndSend(user,
                     purpose,
                     5,
                     "Your OTP code for login",
                     null);
        return createAndSend(user,
                purpose,
                5,
                "Your OTP code for reset password",
                null);

    }

    // New flexible variant used by password reset
    /**
     * Creates and sends an OTP with custom TTL and messaging.
     *
     * @param user       the target user
     * @param purpose    the OTP purpose
     * @param ttlMinutes time-to-live in minutes
     * @param subject    email subject to send (if mail configured)
     * @param body       optional email body (fallback is generated)
     * @return the generated OTP code
     * @throws BadRequestException if inputs are missing or invalid
     */
    @Transactional
    public String createAndSend(User user, String purpose, int ttlMinutes,
                                String subject, String body) {
        if (user == null) {
            throw new BadRequestException("User is required to create an OTP");
        }
        if (purpose == null || purpose.isBlank()) {
            throw new BadRequestException("Purpose is required to create an OTP");
        }
        if (ttlMinutes <= 0) {
            throw new BadRequestException("OTP TTL must be positive");
        }
        List<OtpCode> actives = repo.findActiveByUserIdAndPurpose(user.getId(), purpose, Instant.now());
        actives.forEach(c -> c.setConsumedAt(Instant.now()));

        // Generate 6-digit code
        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(0, 1_000_000));

        // Persist
        OtpCode entity = OtpCode.builder()
                .user(user)
                .code(code)
                .purpose(purpose)
                .expiresAt(Instant.now().plusSeconds(ttlMinutes * 60L))
                .build();
        repo.save(entity);

        // Email — fire async so the HTTP response isn't blocked by the SMTP handshake.
        // The OTP is already persisted above, so it is safe even if the email is slightly delayed.
        final String emailTo = user.getEmail();
        final String emailText = body != null ? body
                : ("Your one-time code is: " + code + " (valid " + ttlMinutes + " minutes)");
        if (from != null && !from.isBlank() && emailTo != null) {
            CompletableFuture.runAsync(() -> {
                try {
                    SimpleMailMessage msg = new SimpleMailMessage();
                    msg.setFrom(from);
                    msg.setTo(emailTo);
                    msg.setSubject(subject);
                    msg.setText(emailText);
                    mailSender.send(msg);
                    log.info("OTP email sent to {}", emailTo);
                } catch (Exception ex) {
                    log.warn("OTP email to {} failed: {}", emailTo, ex.toString());
                }
            });
        }

        log.info("OTP created: user={} purpose={}", user.getUsername(), purpose);
        return code;
    }


    /**
     * Verifies an OTP for a user and purpose, marking it consumed if valid.
     *
     * @param user    the user to validate against
     * @param purpose the OTP purpose
     * @param code    the OTP code to verify
     * @throws UnauthorizedException if user is null
     * @throws BadRequestException   if purpose/code are missing
     * @throws OtpRequiredException  if the OTP is invalid or expired
     */
    @Transactional
    public void verifyOtpOrThrow(User user, String purpose, String code) {
        if (user == null) {
            throw new UnauthorizedException("Unknown user");
        }
        if (purpose == null || purpose.isBlank() || code == null || code.isBlank()) {
            throw new BadRequestException("Purpose and code are required");
        }
        Instant now = Instant.now();

        OtpCode latest = repo.findTopByUser_IdAndPurposeOrderByIdDesc(user.getId(), purpose)
                .orElseThrow(() -> new OtpRequiredException("Invalid email or code"));

        boolean invalid = latest.getConsumedAt() != null
                || now.isAfter(latest.getExpiresAt())
                || !latest.getCode().equals(code);

        if (invalid) {
            throw new OtpRequiredException("Invalid email or code");
        }

        latest.setConsumedAt(now);
        repo.save(latest);
    }

    /**
     * Attempts to classify whether a mail failure was caused by connectivity loss.
     */
    private boolean isConnectivityIssue(Throwable ex) {
        Throwable cause = ex;
        while (cause != null) {
            if (cause instanceof UnknownHostException
                    || cause instanceof ConnectException
                    || cause instanceof SocketTimeoutException) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }




}
