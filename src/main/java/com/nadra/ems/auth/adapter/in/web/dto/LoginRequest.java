package com.nadra.ems.auth.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for user login.
 */
public record LoginRequest(

        @NotBlank(message = "Username or email is required")
        String usernameOrEmail,

        @NotBlank(message = "Password is required")
        String password
) {
}
