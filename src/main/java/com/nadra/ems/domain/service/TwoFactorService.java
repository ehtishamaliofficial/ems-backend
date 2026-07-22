package com.nadra.ems.domain.service;

import com.nadra.ems.domain.model.Role;
import com.nadra.ems.domain.model.User;
import com.nadra.ems.domain.port.in.TwoFactorUseCase;
import com.nadra.ems.domain.port.out.RoleRepository;
import com.nadra.ems.domain.port.out.UserRepository;
import com.nadra.ems.common.exception.AuthenticationFailedException;
import com.nadra.ems.common.exception.ResourceNotFoundException;
import dev.samstevens.totp.code.*;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static dev.samstevens.totp.util.Utils.getDataUriForImage;

/**
 * Domain service for TOTP-based two-factor authentication.
 * <p>
 * 2FA is mandatory in this system. Once enabled, it cannot be disabled.
 * Compatible with Google Authenticator, Authy, Microsoft Authenticator,
 * and any other TOTP-compliant authenticator app (RFC 6238).
 */
@Service
@Transactional
public class TwoFactorService implements TwoFactorUseCase {

    private static final Logger log = LoggerFactory.getLogger(TwoFactorService.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final TokenService tokenService;
    private final SecretGenerator secretGenerator;
    private final String issuer;

    public TwoFactorService(UserRepository userRepository,
                            RoleRepository roleRepository,
                            TokenService tokenService,
                            @Value("${app.2fa.issuer:NADRA-EMS}") String issuer) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.tokenService = tokenService;
        this.secretGenerator = new DefaultSecretGenerator(32);
        this.issuer = issuer;
    }

    // ── TwoFactorUseCase ────────────────────────────────────────────────────

    @Override
    public Map<String, String> setupTwoFactor(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // Generate a new TOTP secret
        String secret = secretGenerator.generate();

        // Store the secret (not yet enabled — user must verify first)
        userRepository.updateTwoFactorSecret(userId, secret, false);

        // Build QR code data
        QrData qrData = new QrData.Builder()
                .label(user.getEmail())
                .secret(secret)
                .issuer(issuer)
                .algorithm(HashingAlgorithm.SHA1)
                .digits(6)
                .period(30)
                .build();

        // Generate QR code as data URI
        String qrCodeDataUri;
        try {
            ZxingPngQrGenerator qrGenerator = new ZxingPngQrGenerator();
            byte[] imageData = qrGenerator.generate(qrData);
            String mimeType = qrGenerator.getImageMimeType();
            qrCodeDataUri = getDataUriForImage(imageData, mimeType);
        } catch (QrGenerationException e) {
            log.error("Failed to generate QR code for userId={}", userId, e);
            throw new RuntimeException("Failed to generate QR code", e);
        }

        log.info("2FA setup initiated for userId={}", userId);

        Map<String, String> result = new LinkedHashMap<>();
        result.put("secret", secret);
        result.put("qrCodeDataUri", qrCodeDataUri);
        result.put("manualEntryKey", secret);
        result.put("issuer", issuer);
        result.put("accountName", user.getEmail());
        return result;
    }

    @Override
    public Map<String, Object> enableTwoFactor(Long userId, String totpCode, String clientIp) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (user.getTwoFactorSecret() == null || user.getTwoFactorSecret().isBlank()) {
            throw new AuthenticationFailedException("2FA setup not initiated. Call setup first.");
        }

        // Verify the code before enabling
        if (verifyCode(user.getTwoFactorSecret(), totpCode)) {
            log.warn("2FA enable failed — invalid code for userId={}", userId);
            throw new AuthenticationFailedException("Invalid TOTP code. Cannot enable 2FA.");
        }

        // Enable 2FA
        userRepository.updateTwoFactorSecret(userId, user.getTwoFactorSecret(), true);
        log.info("2FA enabled for userId={}", userId);

        // Load roles and issue full tokens — this completes the mandatory 2FA setup
        List<Role> roles = roleRepository.findRolesByUserId(user.getId());
        user.setRoles(roles);

        return tokenService.issueTokens(user, clientIp);
    }

    // ── Public verification (used by AuthService during login) ──────────────

    /**
     * Verifies a TOTP code against the given secret.
     *
     * @param secret   the TOTP secret
     * @param totpCode the 6-digit code from the authenticator app
     * @return true if the code is <strong>invalid</strong>
     */
    public boolean verifyCode(String secret, String totpCode) {
        TimeProvider timeProvider = new SystemTimeProvider();
        CodeGenerator codeGenerator = new DefaultCodeGenerator();
        CodeVerifier verifier = new DefaultCodeVerifier(codeGenerator, timeProvider);
        return !verifier.isValidCode(secret, totpCode);
    }
}
