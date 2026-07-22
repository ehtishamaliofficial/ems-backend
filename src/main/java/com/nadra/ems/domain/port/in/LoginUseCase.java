package com.nadra.ems.domain.port.in;

import java.util.Map;

/**
 * Driving port — use case for user login/authentication.
 * <p>
 * The login flow <strong>always</strong> returns a scoped token. Full access tokens
 * are never issued at login — the user must complete 2FA first.
 */
public interface LoginUseCase {

    /**
     * Authenticates a user with username/email and password.
     * <p>
     * Always returns a scoped token:
     * <ul>
     *     <li>If 2FA is enabled → scope {@code 2FA_VERIFY} (user must verify TOTP)</li>
     *     <li>If 2FA is not enabled → scope {@code 2FA_SETUP} (user must setup 2FA first)</li>
     * </ul>
     *
     * @param usernameOrEmail the login identifier
     * @param rawPassword     the plain-text password
     * @param clientIp        the client's IP address (for audit logging)
     * @return map with {@code token}, {@code twoFactorEnabled}
     */
    Map<String, Object> login(String usernameOrEmail, String rawPassword, String clientIp);

    /**
     * Completes a 2FA-protected login by verifying the TOTP code.
     * Issues full access + refresh tokens on success.
     *
     * @param twoFactorToken the scoped token issued after password verification
     * @param totpCode       the 6-digit TOTP code from the authenticator app
     * @param clientIp       the client's IP address
     * @return map with {@code accessToken}, {@code refreshToken}, {@code tokenType}, {@code expiresIn}
     */
    Map<String, Object> verifyTwoFactorLogin(String twoFactorToken, String totpCode, String clientIp);
}
