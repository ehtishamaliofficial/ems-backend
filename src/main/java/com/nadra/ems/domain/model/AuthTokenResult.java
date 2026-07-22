package com.nadra.ems.domain.model;

/**
 * Domain model representing issued authentication token pair and expiration.
 *
 * @param accessToken  JWT access token
 * @param refreshToken JWT refresh token
 * @param tokenType    token type header value (e.g. "Bearer")
 * @param expiresIn    access token expiration time in seconds
 */
public record AuthTokenResult(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn
) {
    public AuthTokenResult(String accessToken, String refreshToken, long expiresIn) {
        this(accessToken, refreshToken, "Bearer", expiresIn);
    }
}
