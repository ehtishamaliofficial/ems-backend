package com.nadra.ems.auth.domain.port.in;

import java.util.Map;

/**
 * Driving port — use case for user login/authentication.
 * <p>
 * The login flow may return JWT tokens directly, or signal that 2FA is required.
 */
public interface LoginUseCase {

    /**
     * Authenticates a user with username/email and password.
     * <p>
     * Returns a map containing either:
     * <ul>
     *     <li>{@code accessToken}, {@code refreshToken} — if login is complete</li>
     *     <li>{@code twoFactorRequired=true}, {@code twoFactorToken} — if 2FA is needed</li>
     * </ul>
     *
     * @param usernameOrEmail the login identifier
     * @param rawPassword     the plain-text password
     * @param clientIp        the client's IP address (for audit logging)
     * @return authentication result map
     */
    Map<String, Object> login(String usernameOrEmail, String rawPassword, String clientIp);

    /**
     * Completes a 2FA-protected login by verifying the TOTP code.
     *
     * @param twoFactorToken the temporary token issued after password verification
     * @param totpCode       the 6-digit TOTP code from the authenticator app
     * @param clientIp       the client's IP address
     * @return map with {@code accessToken} and {@code refreshToken}
     */
    Map<String, Object> verifyTwoFactorLogin(String twoFactorToken, String totpCode, String clientIp);
}
