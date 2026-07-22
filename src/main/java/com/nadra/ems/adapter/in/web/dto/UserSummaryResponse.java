package com.nadra.ems.adapter.in.web.dto;

import java.util.List;

/**
 * Response DTO containing user summary information returned upon registration.
 */
public record UserSummaryResponse(
        Long id,
        String erpNo,
        String username,
        String email,
        String fullName,
        List<String> roles
) {
}
