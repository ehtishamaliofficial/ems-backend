package com.nadra.ems.auth.domain.port.out;

import com.nadra.ems.auth.domain.model.RefreshToken;

import java.util.Optional;

/**
 * Driven port (SPI) — repository contract for RefreshToken persistence.
 */
public interface RefreshTokenRepository {

    /**
     * Persists a new refresh token.
     */
    void save(RefreshToken token);

    /**
     * Finds a refresh token by its hash.
     */
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * Revokes all refresh tokens for a user (e.g. on password change).
     */
    void revokeByUserId(Long userId);

    /**
     * Revokes a specific refresh token by its hash.
     */
    void revokeByTokenHash(String tokenHash);

    /**
     * Deletes expired tokens (housekeeping).
     */
    int deleteExpired();
}
