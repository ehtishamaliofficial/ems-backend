package com.nadra.ems.auth.adapter.out.persistence.rowmapper;

import com.nadra.ems.auth.domain.model.User;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Maps a JDBC {@link ResultSet} row to a {@link User} domain model.
 * Handles all 30+ columns of the users table, including null-safe conversions.
 */
public class UserRowMapper implements RowMapper<User> {

    @Override
    public User mapRow(ResultSet rs, int rowNum) throws SQLException {
        User user = new User();

        // Identity
        user.setId(rs.getLong("id"));
        user.setErpNo(rs.getString("erp_no"));
        user.setUsername(rs.getString("username"));
        user.setEmail(rs.getString("email"));
        user.setPasswordHash(rs.getString("password_hash"));

        // Personal information
        user.setFirstName(rs.getString("first_name"));
        user.setLastName(rs.getString("last_name"));
        user.setFatherName(rs.getString("father_name"));
        user.setCnic(rs.getString("cnic"));
        user.setDateOfBirth(toLocalDate(rs, "date_of_birth"));
        user.setGender(rs.getString("gender"));
        user.setMaritalStatus(rs.getString("marital_status"));
        user.setBloodGroup(rs.getString("blood_group"));
        user.setPhoneNumber(rs.getString("phone_number"));
        user.setEmergencyContactName(rs.getString("emergency_contact_name"));
        user.setEmergencyContactPhone(rs.getString("emergency_contact_phone"));

        // Address
        user.setPermanentAddress(rs.getString("permanent_address"));
        user.setCurrentAddress(rs.getString("current_address"));
        user.setCity(rs.getString("city"));
        user.setProvince(rs.getString("province"));
        user.setCountry(rs.getString("country"));
        user.setPostalCode(rs.getString("postal_code"));

        // Employment
        user.setDepartment(rs.getString("department"));
        user.setDesignation(rs.getString("designation"));
        user.setEmploymentType(rs.getString("employment_type"));
        user.setJoiningDate(toLocalDate(rs, "joining_date"));
        user.setConfirmationDate(toLocalDate(rs, "confirmation_date"));
        user.setTerminationDate(toLocalDate(rs, "termination_date"));
        user.setGrade(rs.getString("grade"));
        user.setSalary(rs.getBigDecimal("salary"));
        user.setBankName(rs.getString("bank_name"));
        user.setBankAccountNo(rs.getString("bank_account_no"));
        user.setProfilePictureUrl(rs.getString("profile_picture_url"));

        // Security & Auth
        user.setActive(rs.getBoolean("is_active"));
        user.setEmailVerified(rs.getBoolean("is_email_verified"));
        user.setTwoFactorEnabled(rs.getBoolean("two_factor_enabled"));
        user.setTwoFactorSecret(rs.getString("two_factor_secret"));
        user.setFailedLoginAttempts(rs.getInt("failed_login_attempts"));
        user.setAccountLockedUntil(toInstant(rs, "account_locked_until"));
        user.setLastLoginAt(toInstant(rs, "last_login_at"));
        user.setLastLoginIp(rs.getString("last_login_ip"));
        user.setPasswordChangedAt(toInstant(rs, "password_changed_at"));

        // Audit
        user.setCreatedAt(toInstant(rs, "created_at"));
        user.setUpdatedAt(toInstant(rs, "updated_at"));
        user.setCreatedBy(rs.getString("created_by"));
        user.setUpdatedBy(rs.getString("updated_by"));

        return user;
    }

    private Instant toInstant(ResultSet rs, String column) throws SQLException {
        Timestamp ts = rs.getTimestamp(column);
        return ts != null ? ts.toInstant() : null;
    }

    private LocalDate toLocalDate(ResultSet rs, String column) throws SQLException {
        java.sql.Date date = rs.getDate(column);
        return date != null ? date.toLocalDate() : null;
    }
}
