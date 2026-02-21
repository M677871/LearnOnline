package com.csis231.api.otp;

import com.csis231.api.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * JPA entity representing a one-time password (OTP) issued for a specific
 * {@link com.csis231.api.user.User} and logical purpose.
 *
 * <p>Each record stores the generated code, its purpose, the creation/expiration
 * timestamps and an optional consumption timestamp that is set once the OTP is
 * successfully used.</p>
 */


    @Entity
    @Table(
            name = "otp_codes",
            indexes = @Index(columnList = "user_id,purpose,expires_at")
    )
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public class OtpCode {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @ManyToOne(optional = false, fetch = FetchType.LAZY)
        @JoinColumn(name = "user_id", nullable = false)
        private User user;

        @Column(nullable = false, length = 6)
        private String code;

        @Column(nullable = false, length = 20)
        private String purpose; // e.g. LOGIN_2FA, EMAIL_VERIFY

    /**
     * Time at which this OTP expires and will no longer be accepted.
     */

        @Column(name = "expires_at", nullable = false)
        private Instant expiresAt;

    /**
     * Time at which this OTP was successfully used.
     *
     * <p>If this value is {@code null}, the OTP has not yet been consumed.</p>
     */

        @Column(name = "consumed_at")
        private Instant consumedAt;

    /**
     * Checks whether this OTP is currently valid.
     *
     * @return {@code true} if the code has not been consumed yet and the current
     *         instant is strictly before {@link #expiresAt}; {@code false} otherwise
     */

        public boolean isValidNow()
        {
            return consumedAt == null && Instant.now().isBefore(expiresAt);
        }
    }



