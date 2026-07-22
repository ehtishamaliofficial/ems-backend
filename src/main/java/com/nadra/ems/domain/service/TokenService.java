package com.nadra.ems.domain.service;

import com.nadra.ems.domain.model.RefreshToken;
import com.nadra.ems.domain.model.User;
import com.nadra.ems.domain.port.out.RefreshTokenRepository;
import com.nadra.ems.domain.port.out.UserRepository;
import com.nadra.ems.infrastructure.security.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Shared service for issuing access + refresh token pairs.
 * <p>
 * Extracted from {@link AuthService} to avoid circular dependencies
 * between AuthService and TwoFactorService.
 */
@Service
@Transactional
public class TokenService {

    private static final Logger log = LoggerFactory.getLogger(TokenService.class);

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    public TokenService(JwtTokenProvider jwtTokenProvider,
                        RefreshTokenRepository refreshTokenRepository,
                        UserRepository userRepository) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
    }

    /**
     * Issues full access + refresh tokens for the user.
     * Stores the refresh token hash and updates last login info.
     *
     * @param user     the authenticated user (with roles loaded)
     * @param clientIp the client's IP address for audit
     * @return map with accessToken, refreshToken, tokenType, expiresIn
     */
    public Map<String, Object> issueTokens(User user, String clientIp) {
        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshToken = jwtTokenProvider.generateRefreshToken();

        // Store refresh token hash
        RefreshToken tokenEntity = new RefreshToken(
                user.getId(),
                hashToken(refreshToken),
                Instant.now().plusMillis(jwtTokenProvider.getRefreshTokenExpirationMs())
        );
        refreshTokenRepository.save(tokenEntity);

        // Update last login
        userRepository.updateLastLogin(user.getId(), Instant.now(), clientIp);

        log.info("Tokens issued for userId={}, erpNo={}", user.getId(), user.getErpNo());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("accessToken", accessToken);
        result.put("refreshToken", refreshToken);
        result.put("tokenType", "Bearer");
        result.put("expiresIn", jwtTokenProvider.getAccessTokenExpirationMs() / 1000);
        return result;
    }

    /**
     * SHA-256 hash of the raw refresh token for secure storage.
     */
    public String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
