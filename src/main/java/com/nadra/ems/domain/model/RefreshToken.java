package com.nadra.ems.domain.model;

import lombok.Data;

import java.time.Instant;

/**
 * Core domain model representing a refresh token for JWT rotation.
 * Pure domain object — no framework annotations.
 */
@Data
public class RefreshToken {

    private Long id;
    private Long userId;
    private String tokenHash;
    private Instant expiresAt;
    private Instant createdAt;
    private boolean revoked;

    public RefreshToken() {
    }

    public RefreshToken(Long userId, String tokenHash, Instant expiresAt) {
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.revoked = false;
    }

    /**
     * Checks whether this refresh token has expired.
     */
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    /**
     * Checks whether this token is still valid (not expired and not revoked).
     */
    public boolean isValid() {
        return !revoked && !isExpired();
    }
}
