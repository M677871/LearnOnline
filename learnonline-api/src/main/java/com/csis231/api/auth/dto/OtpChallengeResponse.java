package com.csis231.api.auth.dto;

import com.csis231.api.otp.model.OtpPurpose;

/**
 * Returned when the password was correct and the login must now be confirmed with an OTP.
 */
public record OtpChallengeResponse(boolean otpRequired, OtpPurpose purpose, String username) {
}
