package com.csis231.api.otp;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link OtpCode} entities.
 *
 * <p>Provides convenience methods to load the latest or currently active OTP
 * codes for a given user and purpose.</p>
 */

public interface OtpCodeRepository extends JpaRepository<OtpCode, Long> {

    /**
     * Returns the most recently persisted OTP code for the given user and purpose.
     *
     * @param userId  database identifier of the user
     * @param purpose logical purpose of the OTP (for example
     *                {@link OtpPurposes#LOGIN_2FA})
     * @return most recent matching {@link OtpCode}, if any
     */

    Optional<OtpCode> findTopByUser_IdAndPurposeOrderByIdDesc(Long userId, String purpose);

    /**
     * Returns all OTP codes for the given user and purpose that are still active.
     *
     * <p>An OTP is considered active if it has not been consumed yet and its
     * expiration time is strictly in the future.</p>
     *
     * @param userId  database identifier of the user
     * @param purpose logical purpose of the OTP
     * @param now     the reference instant used to test expiration
     * @return list of currently active OTP codes, possibly empty
     */

    @Query("""
           select c
           from OtpCode c
           where c.user.id = :userId
             and c.purpose = :purpose
             and c.consumedAt is null
             and c.expiresAt > :now
           """)

    List<OtpCode> findActiveByUserIdAndPurpose(@Param("userId") Long userId,
                                               @Param("purpose") String purpose,
                                               @Param("now") Instant now);

}
