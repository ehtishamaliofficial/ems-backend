package com.nadra.ems.adapter.in.web.dto;

import com.nadra.ems.domain.model.AuthTokenResult;

/**
 * Response DTO for endpoints that issue full access + refresh tokens
 * (verify-2fa, 2fa/enable, refresh).
 */
public record TokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn
) {

    /**
     * Creates a TokenResponse from an AuthTokenResult domain model.
     */
    public static TokenResponse fromDomain(AuthTokenResult result) {
        if (result == null) {
            return null;
        }
        return new TokenResponse(
                result.accessToken(),
                result.refreshToken(),
                result.tokenType(),
                result.expiresIn()
        );
    }
}
