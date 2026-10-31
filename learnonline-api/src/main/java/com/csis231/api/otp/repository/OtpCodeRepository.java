package com.csis231.api.otp.repository;

import com.csis231.api.otp.model.OtpCode;
import com.csis231.api.otp.model.OtpPurpose;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface OtpCodeRepository extends JpaRepository<OtpCode, Long> {

    Optional<OtpCode> findTopByUser_IdAndPurposeOrderByIdDesc(Long userId, OtpPurpose purpose);

    @Query("""
           select c
           from OtpCode c
           where c.user.id = :userId
             and c.purpose = :purpose
             and c.consumedAt is null
             and c.expiresAt > :now
           """)
    List<OtpCode> findActiveByUserIdAndPurpose(@Param("userId") Long userId,
                                               @Param("purpose") OtpPurpose purpose,
                                               @Param("now") Instant now);
}
