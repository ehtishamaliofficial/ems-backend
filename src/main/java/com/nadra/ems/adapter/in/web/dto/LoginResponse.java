package com.nadra.ems.adapter.in.web.dto;

/**
 * Response DTO for the login endpoint.
 * <p>
 * Login always returns a scoped token and a flag indicating whether
 * 2FA is already enabled (so the client knows which flow to follow).
 *
 * @param token            scoped JWT token (restricted to 2FA operations only)
 * @param twoFactorEnabled true if user has 2FA set up (→ go to verify-2fa),
 *                         false if user needs to set up 2FA first (→ go to 2fa/setup)
 */
public record LoginResponse(
        String token,
        boolean twoFactorEnabled
) {
}
