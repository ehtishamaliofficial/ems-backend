package com.nadra.ems.auth.domain.port.in;

import java.util.Map;

/**
 * Driving port — use case for managing TOTP-based two-factor authentication.
 */
public interface TwoFactorUseCase {

    /**
     * Generates a new TOTP secret and returns setup information
     * (secret key + QR code URI) for the authenticator app.
     *
     * @param userId the user's ID
     * @return map containing {@code secret} and {@code qrCodeUri}
     */
    Map<String, String> setupTwoFactor(Long userId);

    /**
     * Enables 2FA for the user after verifying the TOTP code from their authenticator app.
     *
     * @param userId   the user's ID
     * @param totpCode the 6-digit code from the authenticator app
     * @return true if 2FA was successfully enabled
     */
    boolean enableTwoFactor(Long userId, String totpCode);

    /**
     * Disables 2FA for the user after verifying the TOTP code.
     *
     * @param userId   the user's ID
     * @param totpCode the 6-digit code to confirm identity
     * @return true if 2FA was successfully disabled
     */
    boolean disableTwoFactor(Long userId, String totpCode);
}
