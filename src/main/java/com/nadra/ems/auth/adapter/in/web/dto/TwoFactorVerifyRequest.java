package com.nadra.ems.auth.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for two-factor authentication verification during login.
 */
public record TwoFactorVerifyRequest(

        @NotBlank(message = "Two-factor token is required")
        String twoFactorToken,

        @NotBlank(message = "TOTP code is required")
        @Size(min = 6, max = 6, message = "TOTP code must be exactly 6 digits")
        String totpCode
) {
}
