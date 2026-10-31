package com.csis231.api.otp.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * The flow a code was issued for. A code issued for one purpose can never be redeemed for another.
 */
@Getter
@RequiredArgsConstructor
public enum OtpPurpose {
    LOGIN_2FA("Your OTP code for login"),
    PASSWORD_RESET("Your OTP code for reset password");

    private final String emailSubject;
}
