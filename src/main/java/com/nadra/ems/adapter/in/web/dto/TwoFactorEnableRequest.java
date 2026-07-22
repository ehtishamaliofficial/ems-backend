package com.nadra.ems.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for enabling 2FA with a TOTP code.
 */
public record TwoFactorEnableRequest(

        @NotBlank(message = "TOTP code is required")
        @Size(min = 6, max = 6, message = "TOTP code must be exactly 6 digits")
        String totpCode
) {
}
