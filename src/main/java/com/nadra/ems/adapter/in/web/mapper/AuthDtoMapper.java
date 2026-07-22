package com.nadra.ems.adapter.in.web.mapper;

import com.nadra.ems.adapter.in.web.dto.*;
import com.nadra.ems.domain.model.*;

import java.util.List;

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
     * Maps a {@link TwoFactorSetupResult} domain model to a {@link TwoFactorSetupResponse} DTO.
     */
    public static TwoFactorSetupResponse toTwoFactorSetupResponse(TwoFactorSetupResult setupResult) {
        if (setupResult == null) {
            return null;
        }
        return new TwoFactorSetupResponse(
                setupResult.secret(),
                setupResult.qrCodeDataUri(),
                setupResult.manualEntryKey(),
                setupResult.issuer(),
                setupResult.accountName()
        );
    }

    /**
     * Builds a user summary response DTO from a domain User (for registration response).
     */
    public static UserSummaryResponse toUserSummary(User user) {
        if (user == null) {
            return null;
        }
        String fullName = (user.getFirstName() != null ? user.getFirstName() : "") +
                (user.getLastName() != null && !user.getLastName().isBlank() ? " " + user.getLastName() : "");
        return new UserSummaryResponse(
                user.getId(),
                user.getErpNo(),
                user.getUsername(),
                user.getEmail(),
                fullName.trim(),
                user.getRoleNames() != null ? List.copyOf(user.getRoleNames()) : List.of()
        );
    }

    /**
     * Maps {@link LoginResult} domain model to a {@link LoginResponse} DTO.
     */
    public static LoginResponse toLoginResponse(LoginResult result) {
        if (result == null) {
            return null;
        }
        return new LoginResponse(
                result.token(),
                result.twoFactorEnabled()
        );
    }

    /**
     * Maps {@link AuthTokenResult} domain model to a {@link TokenResponse} DTO.
     */
    public static TokenResponse toTokenResponse(AuthTokenResult result) {
        if (result == null) {
            return null;
        }
        return new TokenResponse(
                result.accessToken(),
                result.refreshToken(),
                result.tokenType(),
                result.expiresIn()
        );
    }

    /**
     * Maps {@link AuthTokenResult} domain model to a {@link RefreshTokenResponse} DTO.
     */
    public static RefreshTokenResponse toRefreshTokenResponse(AuthTokenResult result) {
        if (result == null) {
            return null;
        }
        return new RefreshTokenResponse(
                result.accessToken(),
                result.refreshToken(),
                result.tokenType()
        );
    }
}
