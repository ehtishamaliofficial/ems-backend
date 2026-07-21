package com.nadra.ems.infrastructure.security;

import com.nadra.ems.domain.model.User;
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
     * Generates a short-lived scoped JWT token.
     * <p>
     * Scoped tokens restrict the bearer to specific endpoints:
     * <ul>
     *     <li>{@code 2FA_SETUP} — allows only {@code /2fa/setup} and {@code /2fa/enable}</li>
     *     <li>{@code 2FA_VERIFY} — allows only {@code /verify-2fa}</li>
     * </ul>
     *
     * @param userId the user's ID
     * @param scope  the scope string (e.g., "2FA_SETUP", "2FA_VERIFY")
     * @return a signed JWT with the given scope, valid for 5 minutes
     */
    public String generateScopedToken(Long userId, String scope) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + 300_000); // 5 minutes

        return Jwts.builder()
                .subject(userId.toString())
                .claim("scope", scope)
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
     * Validates a scoped token and checks it matches the expected scope.
     *
     * @param token         the JWT token
     * @param expectedScope the expected scope (e.g., "2FA_SETUP", "2FA_VERIFY")
     * @return the userId if valid and scope matches, or null otherwise
     */
    public Long validateScopedToken(String token, String expectedScope) {
        try {
            Claims claims = Jwts.parser().verifyWith(signingKey).build()
                    .parseSignedClaims(token).getPayload();

            String scope = claims.get("scope", String.class);
            if (!expectedScope.equals(scope)) {
                log.warn("Token scope mismatch: expected={}, actual={}", expectedScope, scope);
                return null;
            }

            return Long.parseLong(claims.getSubject());
        } catch (Exception ex) {
            log.warn("Scoped token validation failed: {}", ex.getMessage());
            return null;
        }
    }

    /**
     * Extracts the scope claim from a valid token, or null if not a scoped token.
     */
    public String getTokenScope(String token) {
        try {
            Claims claims = getClaims(token);
            return claims.get("scope", String.class);
        } catch (Exception ex) {
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
