package com.nadra.ems.domain.model;

/**
 * Domain model representing the result of an initial login attempt.
 *
 * @param token            scoped JWT token (restricted to 2FA operations)
 * @param twoFactorEnabled flag indicating whether 2FA is already enabled for the user
 */
public record LoginResult(
        String token,
        boolean twoFactorEnabled
) {
}
