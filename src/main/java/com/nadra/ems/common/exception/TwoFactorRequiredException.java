package com.nadra.ems.common.exception;

import lombok.Getter;

/**
 * Thrown when a login attempt requires a second factor (TOTP) to complete.
 * This is not an error — it signals the client to prompt for a 2FA code.
 */
@Getter
public class TwoFactorRequiredException extends RuntimeException {

    private final String twoFactorToken;

    public TwoFactorRequiredException(String twoFactorToken) {
        super("Two-factor authentication is required to complete login");
        this.twoFactorToken = twoFactorToken;
    }

}
