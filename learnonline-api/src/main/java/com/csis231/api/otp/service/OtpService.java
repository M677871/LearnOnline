package com.csis231.api.otp.service;

import com.csis231.api.otp.exception.InvalidOtpException;
import com.csis231.api.otp.model.OtpCode;
import com.csis231.api.otp.model.OtpPurpose;
import com.csis231.api.otp.repository.OtpCodeRepository;
import com.csis231.api.user.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Issues one-time codes and verifies them.
 *
 * <p>Only the newest code of a user and purpose can be redeemed, and each code works once. After
 * {@value #MAX_FAILED_ATTEMPTS} wrong guesses verification is locked, until the lock expires or a new
 * code is issued. Failed attempts are counted in memory, so they reset on restart and are not shared
 * between application instances.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private static final Duration CODE_LIFETIME = Duration.ofMinutes(5);
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final Duration LOCK_DURATION = Duration.ofMinutes(10);

    private final SecureRandom random = new SecureRandom();
    private final Map<String, AttemptState> failedAttempts = new ConcurrentHashMap<>();
    private final OtpCodeRepository otpCodeRepository;
    private final JavaMailSender mailSender;

    @Value("${mail.from:}")
    private String from;

    /**
     * Replaces any active code of this user and purpose with a new one and e-mails it.
     */
    @Transactional
    public void createAndSend(User user, OtpPurpose purpose) {
        Instant now = Instant.now();
        otpCodeRepository.findActiveByUserIdAndPurpose(user.getId(), purpose, now)
                .forEach(active -> active.setConsumedAt(now));

        String code = String.format("%06d", random.nextInt(1_000_000));
        otpCodeRepository.save(OtpCode.builder()
                .user(user)
                .code(code)
                .purpose(purpose)
                .expiresAt(now.plus(CODE_LIFETIME))
                .build());
        failedAttempts.remove(attemptKey(user, purpose));

        sendAsync(user.getEmail(), purpose, code);
        log.info("OTP created: user={} purpose={}", user.getUsername(), purpose);
    }

    /**
     * Redeems a code, marking it as consumed.
     *
     * @throws InvalidOtpException if the code is wrong, expired or already used, or verification is locked
     */
    @Transactional
    public void verifyOrThrow(User user, OtpPurpose purpose, String code) {
        Instant now = Instant.now();
        String attemptKey = attemptKey(user, purpose);
        if (isLocked(attemptKey, now)) {
            throw new InvalidOtpException("Too many invalid OTP attempts. Please wait and request a new OTP.");
        }

        OtpCode latest = otpCodeRepository.findTopByUser_IdAndPurposeOrderByIdDesc(user.getId(), purpose)
                .orElseThrow(InvalidOtpException::new);
        if (!latest.isUsableAt(now) || !latest.getCode().equals(code)) {
            registerFailedAttempt(attemptKey, now);
            throw new InvalidOtpException();
        }

        latest.setConsumedAt(now);
        failedAttempts.remove(attemptKey);
    }

    /**
     * Sends on another thread so that the HTTP response does not wait for the SMTP handshake.
     * The code is already stored, and a failed delivery only means the user has to request a new one.
     */
    private void sendAsync(String to, OtpPurpose purpose, String code) {
        if (from.isBlank()) {
            log.warn("mail.from is not configured, OTP email to {} skipped", to);
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(purpose.getEmailSubject());
        message.setText("Your one-time code is: " + code + " (valid " + CODE_LIFETIME.toMinutes() + " minutes)");

        CompletableFuture.runAsync(() -> {
            try {
                mailSender.send(message);
                log.info("OTP email sent to {}", to);
            } catch (Exception ex) {
                log.warn("OTP email to {} failed: {}", to, ex.toString());
            }
        });
    }

    private boolean isLocked(String attemptKey, Instant now) {
        AttemptState state = failedAttempts.get(attemptKey);
        if (state == null || state.lockedUntil() == null) {
            return false;
        }
        if (now.isBefore(state.lockedUntil())) {
            return true;
        }
        failedAttempts.remove(attemptKey);
        return false;
    }

    private void registerFailedAttempt(String attemptKey, Instant now) {
        failedAttempts.compute(attemptKey, (key, existing) -> {
            int attempts = existing == null ? 1 : existing.attempts() + 1;
            Instant lockedUntil = attempts >= MAX_FAILED_ATTEMPTS ? now.plus(LOCK_DURATION) : null;
            return new AttemptState(attempts, lockedUntil);
        });
    }

    private static String attemptKey(User user, OtpPurpose purpose) {
        return user.getId() + ":" + purpose;
    }

    private record AttemptState(int attempts, Instant lockedUntil) {
    }
}
