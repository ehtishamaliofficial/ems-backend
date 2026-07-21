package com.nadra.ems.adapter.in.web.dto;

/**
 * Response DTO for 2FA setup — contains secret and QR code data URI.
 */
public record TwoFactorSetupResponse(
        String secret,
        String qrCodeDataUri,
        String manualEntryKey,
        String issuer,
        String accountName
) {
}
