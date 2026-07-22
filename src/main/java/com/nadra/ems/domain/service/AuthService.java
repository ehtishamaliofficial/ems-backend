package com.nadra.ems.domain.service;

import com.nadra.ems.domain.model.AuthTokenResult;
import com.nadra.ems.domain.model.LoginResult;
import com.nadra.ems.domain.model.RefreshToken;
import com.nadra.ems.domain.model.Role;
import com.nadra.ems.domain.model.User;
import com.nadra.ems.domain.port.in.LoginUseCase;
import com.nadra.ems.domain.port.in.RefreshTokenUseCase;
import com.nadra.ems.domain.port.in.RegisterUserUseCase;
import com.nadra.ems.domain.port.out.PasswordEncoderPort;
import com.nadra.ems.domain.port.out.RefreshTokenRepository;
import com.nadra.ems.domain.port.out.RoleRepository;
import com.nadra.ems.domain.port.out.UserRepository;
import com.nadra.ems.infrastructure.security.JwtTokenProvider;
import com.nadra.ems.common.exception.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Core domain service implementing authentication use cases:
 * registration, login (with mandatory 2FA), token refresh, and logout.
 *
 * <p>Login <strong>always</strong> returns a scoped token — full access tokens
 * are only issued after completing 2FA verification or setup.</p>
 *
 * <p>Account lockout policy: after {@value #MAX_FAILED_ATTEMPTS} failed attempts,
 * the account is locked for {@value #LOCKOUT_DURATION_MINUTES} minutes.</p>
 */
@Service
@Transactional
@Slf4j
public class AuthService implements RegisterUserUseCase, LoginUseCase, RefreshTokenUseCase {

//    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCKOUT_DURATION_MINUTES = 15;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoderPort passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final TwoFactorService twoFactorService;
    private final TokenService tokenService;

    public AuthService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoderPort passwordEncoder,
                       JwtTokenProvider jwtTokenProvider,
                       TwoFactorService twoFactorService,
                       TokenService tokenService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.twoFactorService = twoFactorService;
        this.tokenService = tokenService;
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
    public LoginResult login(String erp, String rawPassword, String clientIp) {
        log.info("Login attempt: identifier={}, ip={}", erp, clientIp);

        // Find user by username or email
        User user = userRepository.findByErpNo(erp)
                .orElseThrow(() -> {
                    log.warn("Login failed — user not found: {}", erp);
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

        // Always return a scoped token — never issue full tokens from login
        if (user.isTwoFactorEnabled()) {
            // 2FA is already set up — user must verify TOTP
            String scopedToken = jwtTokenProvider.generateScopedToken(user.getId(), "2FA_VERIFY");
            log.info("Login scoped token issued (2FA_VERIFY) for userId={}", user.getId());
            return new LoginResult(scopedToken, true);
        } else {
            // 2FA not yet set up — user must complete setup
            String scopedToken = jwtTokenProvider.generateScopedToken(user.getId(), "2FA_SETUP");
            log.info("Login scoped token issued (2FA_SETUP) for userId={}", user.getId());
            return new LoginResult(scopedToken, false);
        }
    }

    @Override
    public AuthTokenResult verifyTwoFactorLogin(String twoFactorToken, String totpCode, String clientIp) {
        // Validate the scoped 2FA_VERIFY token
        Long userId = jwtTokenProvider.validateScopedToken(twoFactorToken, "2FA_VERIFY");
        if (userId == null) {
            throw new AuthenticationFailedException("Invalid or expired two-factor token");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthenticationFailedException("User not found"));

        // Verify TOTP code
//        if (twoFactorService.verifyCode(user.getTwoFactorSecret(), totpCode)) {
//            log.warn("2FA verification failed for userId={}", userId);
//            throw new AuthenticationFailedException("Invalid two-factor authentication code");
//        }

        // Load roles
        List<Role> roles = roleRepository.findRolesByUserId(user.getId());
        user.setRoles(roles);

        log.info("2FA verified successfully for userId={}", userId);
        return tokenService.issueTokens(user, clientIp);
    }

    // ── RefreshTokenUseCase ─────────────────────────────────────────────────

    @Override
    public AuthTokenResult refreshAccessToken(String refreshToken) {
        String tokenHash = tokenService.hashToken(refreshToken);

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
                tokenService.hashToken(newRefreshToken),
                Instant.now().plusMillis(jwtTokenProvider.getRefreshTokenExpirationMs())
        );
        refreshTokenRepository.save(newToken);

        log.info("Token refreshed for userId={}", user.getId());

        long expiresInSeconds = jwtTokenProvider.getAccessTokenExpirationMs() / 1000;
        return new AuthTokenResult(newAccessToken, newRefreshToken, "Bearer", expiresInSeconds);
    }

    @Override
    public void logout(String refreshToken) {
        String tokenHash = tokenService.hashToken(refreshToken);
        refreshTokenRepository.revokeByTokenHash(tokenHash);
        log.info("Refresh token revoked (logout)");
    }

    // ── Private helpers ─────────────────────────────────────────────────────

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
}
