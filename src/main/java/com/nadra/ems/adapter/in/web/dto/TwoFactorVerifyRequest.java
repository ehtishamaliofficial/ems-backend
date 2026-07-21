package com.nadra.ems.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for two-factor authentication verification during login.
 * <p>
 * The userId is extracted from the scoped Bearer token — no need to
 * pass the token in the request body.
 */
public record TwoFactorVerifyRequest(

        @NotBlank(message = "TOTP code is required")
        @Size(min = 6, max = 6, message = "TOTP code must be exactly 6 digits")
        String totpCode
) {
}
