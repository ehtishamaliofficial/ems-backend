package com.nadra.ems.auth.domain.service;

import com.nadra.ems.auth.domain.model.RefreshToken;
import com.nadra.ems.auth.domain.model.Role;
import com.nadra.ems.auth.domain.model.User;
import com.nadra.ems.auth.domain.port.in.LoginUseCase;
import com.nadra.ems.auth.domain.port.in.RefreshTokenUseCase;
import com.nadra.ems.auth.domain.port.in.RegisterUserUseCase;
import com.nadra.ems.auth.domain.port.out.PasswordEncoderPort;
import com.nadra.ems.auth.domain.port.out.RefreshTokenRepository;
import com.nadra.ems.auth.domain.port.out.RoleRepository;
import com.nadra.ems.auth.domain.port.out.UserRepository;
import com.nadra.ems.auth.infrastructure.security.JwtTokenProvider;
import com.nadra.ems.common.exception.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;

/**
 * Core domain service implementing authentication use cases:
 * registration, login (with 2FA challenge), token refresh, and logout.
 *
 * <p>Account lockout policy: after {@value #MAX_FAILED_ATTEMPTS} failed attempts,
 * the account is locked for {@value #LOCKOUT_DURATION_MINUTES} minutes.</p>
 */
@Service
@Transactional
public class AuthService implements RegisterUserUseCase, LoginUseCase, RefreshTokenUseCase {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCKOUT_DURATION_MINUTES = 15;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoderPort passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final TwoFactorService twoFactorService;

    public AuthService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoderPort passwordEncoder,
                       JwtTokenProvider jwtTokenProvider,
                       TwoFactorService twoFactorService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.twoFactorService = twoFactorService;
    }

    // ── RegisterUserUseCase ─────────────────────────────────────────────────

    @Override
    public User register(User user, String rawPassword, String roleName) {
        log.info("Registering new user: username={}, erpNo={}", user.getUsername(), user.getErpNo());

        // Uniqueness checks
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new DuplicateResourceException("User", "username", user.getUsername());
        }
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new DuplicateResourceException("User", "email", user.getEmail());
        }
        if (userRepository.existsByErpNo(user.getErpNo())) {
            throw new DuplicateResourceException("User", "erpNo", user.getErpNo());
        }
        if (user.getCnic() != null && !user.getCnic().isBlank() && userRepository.existsByCnic(user.getCnic())) {
            throw new DuplicateResourceException("User", "cnic", user.getCnic());
        }

        // Hash password and save
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setActive(true);
        user.setPasswordChangedAt(Instant.now());
        User savedUser = userRepository.save(user);

        // Assign role
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "name", roleName));
        roleRepository.assignRoleToUser(savedUser.getId(), role.getId());
        savedUser.setRoles(List.of(role));

        log.info("User registered successfully: id={}, erpNo={}", savedUser.getId(), savedUser.getErpNo());
        return savedUser;
    }

    // ── LoginUseCase ────────────────────────────────────────────────────────

    @Override
    public Map<String, Object> login(String usernameOrEmail, String rawPassword, String clientIp) {
        log.info("Login attempt: identifier={}, ip={}", usernameOrEmail, clientIp);

        // Find user by username or email
        User user = userRepository.findByUsername(usernameOrEmail)
                .or(() -> userRepository.findByEmail(usernameOrEmail))
                .orElseThrow(() -> {
                    log.warn("Login failed — user not found: {}", usernameOrEmail);
                    return new AuthenticationFailedException("Invalid credentials");
                });

        // Check account status
        if (!user.isActive()) {
            log.warn("Login failed — account inactive: userId={}", user.getId());
            throw new AuthenticationFailedException("Account is deactivated. Contact administrator.");
        }

        if (user.isAccountLocked()) {
            log.warn("Login failed — account locked: userId={}, lockedUntil={}",
                    user.getId(), user.getAccountLockedUntil());
            throw new AccountLockedException(
                    "Account is locked due to too many failed attempts. Try again later.",
                    user.getAccountLockedUntil());
        }

        // Verify password
        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            handleFailedLogin(user);
            throw new AuthenticationFailedException("Invalid credentials");
        }

        // Password correct — reset failed attempts
        userRepository.resetFailedAttempts(user.getId());

        // Load roles
        List<Role> roles = roleRepository.findRolesByUserId(user.getId());
        user.setRoles(roles);

        // Check if 2FA is required
        if (user.isTwoFactorEnabled()) {
            String twoFactorToken = jwtTokenProvider.generateTwoFactorToken(user.getId());
            log.info("2FA required for userId={}", user.getId());

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("twoFactorRequired", true);
            result.put("twoFactorToken", twoFactorToken);
            return result;
        }

        // No 2FA — issue tokens directly
        return issueTokens(user, clientIp);
    }

    @Override
    public Map<String, Object> verifyTwoFactorLogin(String twoFactorToken, String totpCode, String clientIp) {
        // Validate the temporary 2FA token
        Long userId = jwtTokenProvider.validateTwoFactorToken(twoFactorToken);
        if (userId == null) {
            throw new AuthenticationFailedException("Invalid or expired two-factor token");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthenticationFailedException("User not found"));

        // Verify TOTP code
        if (twoFactorService.verifyCode(user.getTwoFactorSecret(), totpCode)) {
            log.warn("2FA verification failed for userId={}", userId);
            throw new AuthenticationFailedException("Invalid two-factor authentication code");
        }

        // Load roles
        List<Role> roles = roleRepository.findRolesByUserId(user.getId());
        user.setRoles(roles);

        log.info("2FA verified successfully for userId={}", userId);
        return issueTokens(user, clientIp);
    }

    // ── RefreshTokenUseCase ─────────────────────────────────────────────────

    @Override
    public Map<String, String> refreshAccessToken(String refreshToken) {
        String tokenHash = hashToken(refreshToken);

        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new AuthenticationFailedException("Invalid refresh token"));

        if (!storedToken.isValid()) {
            log.warn("Refresh token invalid: expired={}, revoked={}", storedToken.isExpired(), storedToken.isRevoked());
            throw new AuthenticationFailedException("Refresh token is expired or revoked");
        }

        // Revoke the old token (rotation)
        refreshTokenRepository.revokeByTokenHash(tokenHash);

        // Load user and roles
        User user = userRepository.findById(storedToken.getUserId())
                .orElseThrow(() -> new AuthenticationFailedException("User not found"));
        List<Role> roles = roleRepository.findRolesByUserId(user.getId());
        user.setRoles(roles);

        // Issue new token pair
        String newAccessToken = jwtTokenProvider.generateAccessToken(user);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken();

        // Store new refresh token
        RefreshToken newToken = new RefreshToken(
                user.getId(),
                hashToken(newRefreshToken),
                Instant.now().plusMillis(jwtTokenProvider.getRefreshTokenExpirationMs())
        );
        refreshTokenRepository.save(newToken);

        log.info("Token refreshed for userId={}", user.getId());

        Map<String, String> tokens = new LinkedHashMap<>();
        tokens.put("accessToken", newAccessToken);
        tokens.put("refreshToken", newRefreshToken);
        return tokens;
    }

    @Override
    public void logout(String refreshToken) {
        String tokenHash = hashToken(refreshToken);
        refreshTokenRepository.revokeByTokenHash(tokenHash);
        log.info("Refresh token revoked (logout)");
    }

    // ── Private helpers ─────────────────────────────────────────────────────

    private Map<String, Object> issueTokens(User user, String clientIp) {
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
        result.put("user", Map.of(
                "id", user.getId(),
                "erpNo", user.getErpNo(),
                "username", user.getUsername(),
                "email", user.getEmail(),
                "fullName", user.getFirstName() + " " + user.getLastName(),
                "roles", user.getRoleNames()
        ));
        return result;
    }

    private void handleFailedLogin(User user) {
        int attempts = user.getFailedLoginAttempts() + 1;
        userRepository.incrementFailedAttempts(user.getId());

        if (attempts >= MAX_FAILED_ATTEMPTS) {
            Instant lockedUntil = Instant.now().plusSeconds(LOCKOUT_DURATION_MINUTES * 60L);
            userRepository.lockAccount(user.getId(), lockedUntil);
            log.warn("Account locked: userId={}, attempts={}, lockedUntil={}",
                    user.getId(), attempts, lockedUntil);
        } else {
            log.warn("Failed login: userId={}, attempt={}/{}", user.getId(), attempts, MAX_FAILED_ATTEMPTS);
        }
    }

    /**
     * SHA-256 hash of the raw refresh token for secure storage.
     */
    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
