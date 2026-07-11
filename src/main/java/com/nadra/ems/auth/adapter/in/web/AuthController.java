package com.nadra.ems.auth.adapter.in.web;

import com.nadra.ems.auth.adapter.in.web.dto.*;
import com.nadra.ems.auth.adapter.in.web.mapper.AuthDtoMapper;
import com.nadra.ems.auth.domain.model.User;
import com.nadra.ems.auth.domain.port.in.LoginUseCase;
import com.nadra.ems.auth.domain.port.in.RefreshTokenUseCase;
import com.nadra.ems.auth.domain.port.in.RegisterUserUseCase;
import com.nadra.ems.auth.domain.port.in.TwoFactorUseCase;
import com.nadra.ems.auth.infrastructure.security.JwtTokenProvider;
import com.nadra.ems.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.Map;

/**
 * Driving adapter — REST controller for authentication endpoints.
 * <p>
 * All endpoints are under {@code /api/v1/auth/} and are publicly accessible
 * (configured in {@code SecurityConfig}).
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Endpoints for user registration, login, token refresh, and 2FA management")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final RegisterUserUseCase registerUserUseCase;
    private final LoginUseCase loginUseCase;
    private final TwoFactorUseCase twoFactorUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthController(RegisterUserUseCase registerUserUseCase,
                          LoginUseCase loginUseCase,
                          TwoFactorUseCase twoFactorUseCase,
                          RefreshTokenUseCase refreshTokenUseCase,
                          JwtTokenProvider jwtTokenProvider) {
        this.registerUserUseCase = registerUserUseCase;
        this.loginUseCase = loginUseCase;
        this.twoFactorUseCase = twoFactorUseCase;
        this.refreshTokenUseCase = refreshTokenUseCase;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    // ── Registration ────────────────────────────────────────────────────────

    @Operation(summary = "Register a new user", description = "Creates a new employee account.")
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Map<String, Object>>> register(
            @Valid @RequestBody RegisterRequest request) {

        log.info("Registration request: username={}, erpNo={}", request.username(), request.erpNo());

        User user = AuthDtoMapper.toUser(request);
        String roleName = (request.roleName() != null && !request.roleName().isBlank())
                ? request.roleName() : "EMPLOYEE";

        User created = registerUserUseCase.register(user, request.password(), roleName);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(AuthDtoMapper.toUserSummary(created), "User registered successfully"));
    }

    // ── Login ───────────────────────────────────────────────────────────────

    @Operation(summary = "Login to the system", description = "Authenticates a user by username or email. May return a 2FA requirement if enabled.")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {

        log.info("Login request: identifier={}", request.usernameOrEmail());
        String clientIp = getClientIp(httpRequest);

        Map<String, Object> result = loginUseCase.login(
                request.usernameOrEmail(), request.password(), clientIp);

        LoginResponse response = LoginResponse.fromMap(result);

        if (response.twoFactorRequired()) {
            return ResponseEntity.ok(ApiResponse.success(response, "Two-factor authentication required"));
        }

        return ResponseEntity.ok(ApiResponse.success(response, "Login successful"));
    }

    // ── 2FA Verification (during login) ─────────────────────────────────────

    @Operation(summary = "Verify 2FA token", description = "Verifies the TOTP code provided by the user during the login process.")
    @PostMapping("/verify-2fa")
    public ResponseEntity<ApiResponse<LoginResponse>> verifyTwoFactor(
            @Valid @RequestBody TwoFactorVerifyRequest request,
            HttpServletRequest httpRequest) {

        log.info("2FA verification request");
        String clientIp = getClientIp(httpRequest);

        Map<String, Object> result = loginUseCase.verifyTwoFactorLogin(
                request.twoFactorToken(), request.totpCode(), clientIp);

        LoginResponse response = LoginResponse.fromMap(result);
        return ResponseEntity.ok(ApiResponse.success(response, "Two-factor authentication verified"));
    }

    // ── Token Refresh ───────────────────────────────────────────────────────

    @Operation(summary = "Refresh access token", description = "Generates a new access token using a valid refresh token.")
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<Map<String, String>>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) {

        log.info("Token refresh request");
        Map<String, String> tokens = refreshTokenUseCase.refreshAccessToken(request.refreshToken());
        return ResponseEntity.ok(ApiResponse.success(tokens, "Token refreshed successfully"));
    }

    // ── 2FA Setup ───────────────────────────────────────────────────────────

    @Operation(summary = "Setup 2FA", description = "Generates a 2FA secret and QR code for the authenticated user.", security = @SecurityRequirement(name = "Bearer Authentication"))
    @PostMapping("/2fa/setup")
    public ResponseEntity<ApiResponse<TwoFactorSetupResponse>> setupTwoFactor(
            @RequestHeader("Authorization") String authHeader) {

        Long userId = extractUserIdFromToken(authHeader);
        log.info("2FA setup request for userId={}", userId);

        Map<String, String> setupResult = twoFactorUseCase.setupTwoFactor(userId);
        TwoFactorSetupResponse response = AuthDtoMapper.toTwoFactorSetupResponse(setupResult);

        return ResponseEntity.ok(ApiResponse.success(response, "Scan the QR code with your authenticator app"));
    }

    @Operation(summary = "Enable 2FA", description = "Enables 2FA for the user after validating the provided TOTP code.", security = @SecurityRequirement(name = "Bearer Authentication"))
    @PostMapping("/2fa/enable")
    public ResponseEntity<ApiResponse<Void>> enableTwoFactor(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, String> body) {

        Long userId = extractUserIdFromToken(authHeader);
        String totpCode = body.get("totpCode");
        log.info("2FA enable request for userId={}", userId);

        twoFactorUseCase.enableTwoFactor(userId, totpCode);
        return ResponseEntity.ok(ApiResponse.success(null, "Two-factor authentication enabled successfully"));
    }

    @Operation(summary = "Disable 2FA", description = "Disables 2FA for the user after validating the provided TOTP code.", security = @SecurityRequirement(name = "Bearer Authentication"))
    @PostMapping("/2fa/disable")
    public ResponseEntity<ApiResponse<Void>> disableTwoFactor(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, String> body) {

        Long userId = extractUserIdFromToken(authHeader);
        String totpCode = body.get("totpCode");
        log.info("2FA disable request for userId={}", userId);

        twoFactorUseCase.disableTwoFactor(userId, totpCode);
        return ResponseEntity.ok(ApiResponse.success(null, "Two-factor authentication disabled successfully"));
    }

    // ── Logout ──────────────────────────────────────────────────────────────

    @Operation(summary = "Logout from the system", description = "Invalidates the refresh token to logout the user.")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @Valid @RequestBody RefreshTokenRequest request) {

        log.info("Logout request");
        refreshTokenUseCase.logout(request.refreshToken());
        return ResponseEntity.ok(ApiResponse.success(null, "Logged out successfully"));
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private Long extractUserIdFromToken(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        return jwtTokenProvider.getUserIdFromToken(token);
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
