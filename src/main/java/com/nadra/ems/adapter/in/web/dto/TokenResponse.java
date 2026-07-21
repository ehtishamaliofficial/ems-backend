package com.nadra.ems.adapter.in.web.dto;

import java.util.Map;

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
     * Creates a TokenResponse from the auth service result map.
     */
    public static TokenResponse fromMap(Map<String, Object> result) {
        return new TokenResponse(
                (String) result.get("accessToken"),
                (String) result.get("refreshToken"),
                (String) result.get("tokenType"),
                ((Number) result.get("expiresIn")).longValue()
        );
    }
}
