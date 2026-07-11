package com.nadra.ems.auth.infrastructure.security;

import com.nadra.ems.auth.domain.model.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

/**
 * JWT token provider — generates and validates access, refresh, and 2FA temporary tokens.
 * <p>
 * Uses HMAC-SHA256 signing. Access tokens carry user claims (id, erpNo, username, roles).
 * Refresh tokens are opaque UUIDs (stored hashed in the database).
 */
@Component
@Getter
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    private final SecretKey signingKey;
    private final long accessTokenExpirationMs;
    private final long refreshTokenExpirationMs;

    public JwtTokenProvider(
            @Value("${app.jwt.secret}") String jwtSecret,
            @Value("${app.jwt.access-token-expiration-ms}") long accessTokenExpirationMs,
            @Value("${app.jwt.refresh-token-expiration-ms}") long refreshTokenExpirationMs) {
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(
                java.util.Base64.getEncoder().encodeToString(jwtSecret.getBytes())));
        this.accessTokenExpirationMs = accessTokenExpirationMs;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }

    // ── Token Generation ────────────────────────────────────────────────────

    /**
     * Generates a JWT access token with user claims.
     */
    public String generateAccessToken(User user) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + accessTokenExpirationMs);

        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("erpNo", user.getErpNo())
                .claim("username", user.getUsername())
                .claim("email", user.getEmail())
                .claim("roles", user.getRoleNames())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey)
                .compact();
    }

    /**
     * Generates an opaque refresh token (UUID).
     * The actual token is stored hashed in the database.
     */
    public String generateRefreshToken() {
        return UUID.randomUUID().toString();
    }

    /**
     * Generates a short-lived JWT for the 2FA challenge step.
     * This token only carries the userId and expires in 5 minutes.
     */
    public String generateTwoFactorToken(Long userId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + 300_000); // 5 minutes

        return Jwts.builder()
                .subject(userId.toString())
                .claim("purpose", "2fa")
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey)
                .compact();
    }

    // ── Token Validation ────────────────────────────────────────────────────

    /**
     * Validates a JWT token and returns true if valid.
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException ex) {
            log.warn("JWT expired: {}", ex.getMessage());
        } catch (MalformedJwtException ex) {
            log.warn("JWT malformed: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            log.warn("JWT unsupported: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            log.warn("JWT claims string is empty: {}", ex.getMessage());
        } catch (Exception ex) {
            log.warn("JWT validation failed: {}", ex.getMessage());
        }
        return false;
    }

    /**
     * Validates a 2FA temporary token and returns the userId, or null if invalid.
     */
    public Long validateTwoFactorToken(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(signingKey).build()
                    .parseSignedClaims(token).getPayload();

            if (!"2fa".equals(claims.get("purpose", String.class))) {
                log.warn("Token is not a 2FA token");
                return null;
            }

            return Long.parseLong(claims.getSubject());
        } catch (Exception ex) {
            log.warn("2FA token validation failed: {}", ex.getMessage());
            return null;
        }
    }

    // ── Claim Extraction ────────────────────────────────────────────────────

    /**
     * Extracts the userId (subject) from a valid access token.
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = getClaims(token);
        return Long.parseLong(claims.getSubject());
    }

    /**
     * Extracts the username from a valid access token.
     */
    public String getUsernameFromToken(String token) {
        Claims claims = getClaims(token);
        return claims.get("username", String.class);
    }

    /**
     * Extracts the ERP number from a valid access token.
     */
    public String getErpNoFromToken(String token) {
        Claims claims = getClaims(token);
        return claims.get("erpNo", String.class);
    }

    private Claims getClaims(String token) {
        return Jwts.parser().verifyWith(signingKey).build()
                .parseSignedClaims(token).getPayload();
    }
}
