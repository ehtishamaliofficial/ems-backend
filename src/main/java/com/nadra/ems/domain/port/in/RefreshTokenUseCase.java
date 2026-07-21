package com.nadra.ems.domain.port.in;

import java.util.Map;

/**
 * Driving port — use case for refreshing and revoking JWT tokens.
 */
public interface RefreshTokenUseCase {

    /**
     * Rotates a refresh token: validates the old one, revokes it, and issues new tokens.
     *
     * @param refreshToken the current refresh token string
     * @return map containing new {@code accessToken} and {@code refreshToken}
     */
    Map<String, String> refreshAccessToken(String refreshToken);

    /**
     * Revokes a specific refresh token (logout).
     *
     * @param refreshToken the refresh token to revoke
     */
    void logout(String refreshToken);
}
