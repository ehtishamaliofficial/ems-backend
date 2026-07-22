package com.nadra.ems.domain.model;

import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Core domain model representing an employee/user in the EMS.
 * <p>
 * This is a pure domain object — no Spring or framework annotations.
 * The {@code erpNo} is the business-facing employee identifier;
 * {@code id} is the auto-generated surrogate key.
 */
@Data
public class User {

    // ── Identity ────────────────────────────────────────────────────────────
    private Long id;
    private String erpNo;
    private String username;
    private String email;
    private String passwordHash;

    // ── Personal Information ────────────────────────────────────────────────
    private String firstName;
    private String lastName;
    private String fatherName;
    private String cnic;
    private LocalDate dateOfBirth;
    private String gender;
    private String maritalStatus;
    private String bloodGroup;
    private String phoneNumber;
    private String emergencyContactName;
    private String emergencyContactPhone;

    // ── Address ─────────────────────────────────────────────────────────────
    private String permanentAddress;
    private String currentAddress;
    private String city;
    private String province;
    private String country;
    private String postalCode;

    // ── Employment ──────────────────────────────────────────────────────────
    private String department;
    private String designation;
    private String employmentType;
    private LocalDate joiningDate;
    private LocalDate confirmationDate;
    private LocalDate terminationDate;
    private String grade;
    private BigDecimal salary;
    private String bankName;
    private String bankAccountNo;
    private String profilePictureUrl;

    // ── Security & Auth ─────────────────────────────────────────────────────
    private boolean active;
    private boolean emailVerified;
    private boolean twoFactorEnabled;
    private String twoFactorSecret;
    private int failedLoginAttempts;
    private Instant accountLockedUntil;
    private Instant lastLoginAt;
    private String lastLoginIp;
    private Instant passwordChangedAt;

    // ── Audit ───────────────────────────────────────────────────────────────
    private Instant createdAt;
    private Instant updatedAt;
    private String createdBy;
    private String updatedBy;

    // ── Roles (transient — loaded separately) ───────────────────────────────
    private List<Role> roles = new ArrayList<>();

    public User() {
    }

    // ── Domain behavior ─────────────────────────────────────────────────────

    /**
     * Checks whether this account is currently locked.
     */
    public boolean isAccountLocked() {
        return accountLockedUntil != null && Instant.now().isBefore(accountLockedUntil);
    }

    /**
     * Checks if the user has a specific role.
     */
    public boolean hasRole(String roleName) {
        return roles.stream().anyMatch(r -> r.getName().equalsIgnoreCase(roleName));
    }

    /**
     * Returns role names as a list of strings.
     */
    public List<String> getRoleNames() {
        return roles.stream().map(Role::getName).toList();
    }
}
