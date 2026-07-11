package com.nadra.ems.auth.adapter.in.web.mapper;

import com.nadra.ems.auth.adapter.in.web.dto.RegisterRequest;
import com.nadra.ems.auth.adapter.in.web.dto.TwoFactorSetupResponse;
import com.nadra.ems.auth.domain.model.User;

import java.util.Map;

/**
 * Maps between web DTOs and domain models.
 */
public final class AuthDtoMapper {

    private AuthDtoMapper() {
        // Utility class
    }

    /**
     * Maps a {@link RegisterRequest} to a {@link User} domain model.
     */
    public static User toUser(RegisterRequest request) {
        User user = new User();
        user.setErpNo(request.erpNo());
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setFatherName(request.fatherName());
        user.setCnic(request.cnic());
        user.setPhoneNumber(request.phoneNumber());
        user.setDepartment(request.department());
        user.setDesignation(request.designation());
        user.setEmploymentType(request.employmentType());
        user.setGrade(request.grade());
        return user;
    }

    /**
     * Maps a 2FA setup result map to a {@link TwoFactorSetupResponse} DTO.
     */
    public static TwoFactorSetupResponse toTwoFactorSetupResponse(Map<String, String> setupResult) {
        return new TwoFactorSetupResponse(
                setupResult.get("secret"),
                setupResult.get("qrCodeDataUri"),
                setupResult.get("manualEntryKey"),
                setupResult.get("issuer"),
                setupResult.get("accountName")
        );
    }

    /**
     * Builds a simple user summary map from a domain User (for registration response).
     */
    public static Map<String, Object> toUserSummary(User user) {
        return Map.of(
                "id", user.getId(),
                "erpNo", user.getErpNo(),
                "username", user.getUsername(),
                "email", user.getEmail(),
                "fullName", user.getFirstName() + " " + user.getLastName(),
                "roles", user.getRoleNames()
        );
    }
}
