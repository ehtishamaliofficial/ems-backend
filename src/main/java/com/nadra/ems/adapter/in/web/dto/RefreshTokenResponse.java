package com.nadra.ems.adapter.in.web.dto;

import com.nadra.ems.domain.model.AuthTokenResult;

/**
 * Response DTO for token refresh endpoints.
 */
public record RefreshTokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType
) {
    public RefreshTokenResponse(String accessToken, String refreshToken) {
        this(accessToken, refreshToken, "Bearer");
    }

    public static RefreshTokenResponse fromDomain(AuthTokenResult result) {
        if (result == null) {
            return null;
        }
        return new RefreshTokenResponse(
                result.accessToken(),
                result.refreshToken(),
                result.tokenType()
        );
    }
}
