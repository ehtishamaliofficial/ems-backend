package com.nadra.ems.auth.adapter.in.web.dto;

import java.util.List;
import java.util.Map;

/**
 * Response DTO for successful login.
 */
public record LoginResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        UserInfo user,
        boolean twoFactorRequired,
        String twoFactorToken
) {

    public record UserInfo(
            Long id,
            String erpNo,
            String username,
            String email,
            String fullName,
            List<String> roles
    ) {
    }

    /**
     * Creates a LoginResponse from the auth service result map.
     */
    @SuppressWarnings("unchecked")
    public static LoginResponse fromMap(Map<String, Object> result) {
        if (Boolean.TRUE.equals(result.get("twoFactorRequired"))) {
            return new LoginResponse(
                    null, null, null, 0, null,
                    true,
                    (String) result.get("twoFactorToken")
            );
        }

        Map<String, Object> userMap = (Map<String, Object>) result.get("user");
        UserInfo userInfo = new UserInfo(
                ((Number) userMap.get("id")).longValue(),
                (String) userMap.get("erpNo"),
                (String) userMap.get("username"),
                (String) userMap.get("email"),
                (String) userMap.get("fullName"),
                (List<String>) userMap.get("roles")
        );

        return new LoginResponse(
                (String) result.get("accessToken"),
                (String) result.get("refreshToken"),
                (String) result.get("tokenType"),
                ((Number) result.get("expiresIn")).longValue(),
                userInfo,
                false,
                null
        );
    }
}
