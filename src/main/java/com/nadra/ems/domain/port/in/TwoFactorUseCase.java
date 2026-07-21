package com.nadra.ems.domain.port.in;

import java.util.Map;

/**
 * Driving port — use case for managing TOTP-based two-factor authentication.
 * <p>
 * 2FA is mandatory in this system. Users must complete 2FA setup on first login.
 * There is no option to disable 2FA once enabled.
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
     * On success, issues full access + refresh tokens since this completes the mandatory 2FA flow.
     *
     * @param userId   the user's ID
     * @param totpCode the 6-digit code from the authenticator app
     * @param clientIp the client's IP address (for audit logging)
     * @return map with {@code accessToken}, {@code refreshToken}, {@code tokenType}, {@code expiresIn}
     */
    Map<String, Object> enableTwoFactor(Long userId, String totpCode, String clientIp);
}
