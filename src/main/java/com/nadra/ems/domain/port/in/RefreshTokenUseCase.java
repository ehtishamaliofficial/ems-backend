package com.nadra.ems.domain.port.in;

import com.nadra.ems.domain.model.AuthTokenResult;

/**
 * Driving port — use case for refreshing and revoking JWT tokens.
 */
public interface RefreshTokenUseCase {

    /**
     * Rotates a refresh token: validates the old one, revokes it, and issues new tokens.
     *
     * @param refreshToken the current refresh token string
     * @return {@link AuthTokenResult} containing new access and refresh tokens
     */
    AuthTokenResult refreshAccessToken(String refreshToken);

    /**
     * Revokes a specific refresh token (logout).
     *
     * @param refreshToken the refresh token to revoke
     */
    void logout(String refreshToken);
}
