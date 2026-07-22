package com.nadra.ems.domain.model;

/**
 * Domain model representing 2FA setup information for authenticator app configuration.
 *
 * @param secret         the generated TOTP secret key
 * @param qrCodeDataUri  the QR code PNG encoded as data URI
 * @param manualEntryKey the manual entry key for authenticator app
 * @param issuer         the issuing organization name
 * @param accountName    the user's email/account name
 */
public record TwoFactorSetupResult(
        String secret,
        String qrCodeDataUri,
        String manualEntryKey,
        String issuer,
        String accountName
) {
}
