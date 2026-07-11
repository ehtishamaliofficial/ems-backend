package com.nadra.ems.auth.adapter.out.persistence;

import com.nadra.ems.auth.adapter.out.persistence.exception.DatabaseExceptionTranslator;
import com.nadra.ems.auth.adapter.out.persistence.rowmapper.UserRowMapper;
import com.nadra.ems.auth.domain.model.User;
import com.nadra.ems.auth.domain.port.out.UserRepository;
import com.nadra.ems.common.exception.DuplicateResourceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;

/**
 * JDBC adapter implementing the {@link UserRepository} driven port.
 * Uses Spring {@link JdbcClient} with named parameters for all queries.
 */
@Repository
public class JdbcUserRepository implements UserRepository {

    private static final Logger log = LoggerFactory.getLogger(JdbcUserRepository.class);
    private static final UserRowMapper ROW_MAPPER = new UserRowMapper();

    private final JdbcClient jdbcClient;
    private final DatabaseExceptionTranslator exceptionTranslator;

    public JdbcUserRepository(JdbcClient jdbcClient, DatabaseExceptionTranslator exceptionTranslator) {
        this.jdbcClient = jdbcClient;
        this.exceptionTranslator = exceptionTranslator;
    }

    @Override
    public User save(User user) {
        log.debug("Saving user: username={}, erpNo={}", user.getUsername(), user.getErpNo());

        String sql = """
                INSERT INTO users (
                    erp_no, username, email, password_hash,
                    first_name, last_name, father_name, cnic,
                    date_of_birth, gender, marital_status, blood_group,
                    phone_number, emergency_contact_name, emergency_contact_phone,
                    permanent_address, current_address, city, province, country, postal_code,
                    department, designation, employment_type, joining_date,
                    confirmation_date, termination_date, grade, salary,
                    bank_name, bank_account_no, profile_picture_url,
                    is_active, is_email_verified, two_factor_enabled, two_factor_secret,
                    password_changed_at, created_by, updated_by
                ) VALUES (
                    :erpNo, :username, :email, :passwordHash,
                    :firstName, :lastName, :fatherName, :cnic,
                    :dateOfBirth, :gender, :maritalStatus, :bloodGroup,
                    :phoneNumber, :emergencyContactName, :emergencyContactPhone,
                    :permanentAddress, :currentAddress, :city, :province, :country, :postalCode,
                    :department, :designation, :employmentType, :joiningDate,
                    :confirmationDate, :terminationDate, :grade, :salary,
                    :bankName, :bankAccountNo, :profilePictureUrl,
                    :isActive, :isEmailVerified, :twoFactorEnabled, :twoFactorSecret,
                    :passwordChangedAt, :createdBy, :updatedBy
                )
                """;

        try {
            KeyHolder keyHolder = new GeneratedKeyHolder();

            jdbcClient.sql(sql)
                    .param("erpNo", user.getErpNo())
                    .param("username", user.getUsername())
                    .param("email", user.getEmail())
                    .param("passwordHash", user.getPasswordHash())
                    .param("firstName", user.getFirstName())
                    .param("lastName", user.getLastName())
                    .param("fatherName", user.getFatherName())
                    .param("cnic", user.getCnic())
                    .param("dateOfBirth", user.getDateOfBirth())
                    .param("gender", user.getGender())
                    .param("maritalStatus", user.getMaritalStatus())
                    .param("bloodGroup", user.getBloodGroup())
                    .param("phoneNumber", user.getPhoneNumber())
                    .param("emergencyContactName", user.getEmergencyContactName())
                    .param("emergencyContactPhone", user.getEmergencyContactPhone())
                    .param("permanentAddress", user.getPermanentAddress())
                    .param("currentAddress", user.getCurrentAddress())
                    .param("city", user.getCity())
                    .param("province", user.getProvince())
                    .param("country", user.getCountry())
                    .param("postalCode", user.getPostalCode())
                    .param("department", user.getDepartment())
                    .param("designation", user.getDesignation())
                    .param("employmentType", user.getEmploymentType())
                    .param("joiningDate", user.getJoiningDate())
                    .param("confirmationDate", user.getConfirmationDate())
                    .param("terminationDate", user.getTerminationDate())
                    .param("grade", user.getGrade())
                    .param("salary", user.getSalary())
                    .param("bankName", user.getBankName())
                    .param("bankAccountNo", user.getBankAccountNo())
                    .param("profilePictureUrl", user.getProfilePictureUrl())
                    .param("isActive", user.isActive())
                    .param("isEmailVerified", user.isEmailVerified())
                    .param("twoFactorEnabled", user.isTwoFactorEnabled())
                    .param("twoFactorSecret", user.getTwoFactorSecret())
                    .param("passwordChangedAt", toTimestamp(user.getPasswordChangedAt()))
                    .param("createdBy", user.getCreatedBy())
                    .param("updatedBy", user.getUpdatedBy())
                    .update(keyHolder, "id");

            Number generatedId = keyHolder.getKeyAs(Long.class);
            if (generatedId != null) {
                user.setId(generatedId.longValue());
            }

            log.info("User saved: id={}, erpNo={}", user.getId(), user.getErpNo());
            return user;

        } catch (DuplicateKeyException ex) {
            DuplicateResourceException translated = exceptionTranslator.translateDuplicateKey(ex);
            log.warn("Duplicate key on user save: {}", translated.getMessage());
            throw translated;
        }
    }

    @Override
    public Optional<User> findById(Long id) {
        log.debug("Finding user by id={}", id);
        try {
            User user = jdbcClient.sql("SELECT * FROM users WHERE id = :id")
                    .param("id", id)
                    .query(ROW_MAPPER)
                    .single();
            return Optional.of(user);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<User> findByErpNo(String erpNo) {
        log.debug("Finding user by erpNo={}", erpNo);
        try {
            User user = jdbcClient.sql("SELECT * FROM users WHERE erp_no = :erpNo")
                    .param("erpNo", erpNo)
                    .query(ROW_MAPPER)
                    .single();
            return Optional.of(user);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<User> findByUsername(String username) {
        log.debug("Finding user by username={}", username);
        try {
            User user = jdbcClient.sql("SELECT * FROM users WHERE username = :username")
                    .param("username", username)
                    .query(ROW_MAPPER)
                    .single();
            return Optional.of(user);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<User> findByEmail(String email) {
        log.debug("Finding user by email={}", email);
        try {
            User user = jdbcClient.sql("SELECT * FROM users WHERE email = :email")
                    .param("email", email)
                    .query(ROW_MAPPER)
                    .single();
            return Optional.of(user);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<User> findByCnic(String cnic) {
        log.debug("Finding user by cnic");
        try {
            User user = jdbcClient.sql("SELECT * FROM users WHERE cnic = :cnic")
                    .param("cnic", cnic)
                    .query(ROW_MAPPER)
                    .single();
            return Optional.of(user);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public void update(User user) {
        log.debug("Updating user: id={}", user.getId());
        String sql = """
                UPDATE users SET
                    erp_no = :erpNo, username = :username, email = :email,
                    first_name = :firstName, last_name = :lastName, father_name = :fatherName,
                    cnic = :cnic, date_of_birth = :dateOfBirth, gender = :gender,
                    marital_status = :maritalStatus, blood_group = :bloodGroup,
                    phone_number = :phoneNumber, emergency_contact_name = :emergencyContactName,
                    emergency_contact_phone = :emergencyContactPhone,
                    permanent_address = :permanentAddress, current_address = :currentAddress,
                    city = :city, province = :province, country = :country, postal_code = :postalCode,
                    department = :department, designation = :designation, employment_type = :employmentType,
                    joining_date = :joiningDate, confirmation_date = :confirmationDate,
                    termination_date = :terminationDate, grade = :grade, salary = :salary,
                    bank_name = :bankName, bank_account_no = :bankAccountNo,
                    profile_picture_url = :profilePictureUrl,
                    is_active = :isActive, is_email_verified = :isEmailVerified,
                    updated_at = NOW(), updated_by = :updatedBy
                WHERE id = :id
                """;

        try {
            jdbcClient.sql(sql)
                    .param("id", user.getId())
                    .param("erpNo", user.getErpNo())
                    .param("username", user.getUsername())
                    .param("email", user.getEmail())
                    .param("firstName", user.getFirstName())
                    .param("lastName", user.getLastName())
                    .param("fatherName", user.getFatherName())
                    .param("cnic", user.getCnic())
                    .param("dateOfBirth", user.getDateOfBirth())
                    .param("gender", user.getGender())
                    .param("maritalStatus", user.getMaritalStatus())
                    .param("bloodGroup", user.getBloodGroup())
                    .param("phoneNumber", user.getPhoneNumber())
                    .param("emergencyContactName", user.getEmergencyContactName())
                    .param("emergencyContactPhone", user.getEmergencyContactPhone())
                    .param("permanentAddress", user.getPermanentAddress())
                    .param("currentAddress", user.getCurrentAddress())
                    .param("city", user.getCity())
                    .param("province", user.getProvince())
                    .param("country", user.getCountry())
                    .param("postalCode", user.getPostalCode())
                    .param("department", user.getDepartment())
                    .param("designation", user.getDesignation())
                    .param("employmentType", user.getEmploymentType())
                    .param("joiningDate", user.getJoiningDate())
                    .param("confirmationDate", user.getConfirmationDate())
                    .param("terminationDate", user.getTerminationDate())
                    .param("grade", user.getGrade())
                    .param("salary", user.getSalary())
                    .param("bankName", user.getBankName())
                    .param("bankAccountNo", user.getBankAccountNo())
                    .param("profilePictureUrl", user.getProfilePictureUrl())
                    .param("isActive", user.isActive())
                    .param("isEmailVerified", user.isEmailVerified())
                    .param("updatedBy", user.getUpdatedBy())
                    .update();

            log.info("User updated: id={}", user.getId());
        } catch (DuplicateKeyException ex) {
            throw exceptionTranslator.translateDuplicateKey(ex);
        }
    }

    @Override
    public void updatePassword(Long userId, String passwordHash) {
        log.debug("Updating password for userId={}", userId);
        jdbcClient.sql("UPDATE users SET password_hash = :hash, password_changed_at = NOW(), updated_at = NOW() WHERE id = :id")
                .param("hash", passwordHash)
                .param("id", userId)
                .update();
    }

    @Override
    public void updateTwoFactorSecret(Long userId, String secret, boolean enabled) {
        log.debug("Updating 2FA for userId={}, enabled={}", userId, enabled);
        jdbcClient.sql("UPDATE users SET two_factor_secret = :secret, two_factor_enabled = :enabled, updated_at = NOW() WHERE id = :id")
                .param("secret", secret)
                .param("enabled", enabled)
                .param("id", userId)
                .update();
    }

    @Override
    public void incrementFailedAttempts(Long userId) {
        jdbcClient.sql("UPDATE users SET failed_login_attempts = failed_login_attempts + 1, updated_at = NOW() WHERE id = :id")
                .param("id", userId)
                .update();
    }

    @Override
    public void resetFailedAttempts(Long userId) {
        jdbcClient.sql("UPDATE users SET failed_login_attempts = 0, account_locked_until = NULL, updated_at = NOW() WHERE id = :id")
                .param("id", userId)
                .update();
    }

    @Override
    public void lockAccount(Long userId, Instant lockedUntil) {
        log.warn("Locking account: userId={}, until={}", userId, lockedUntil);
        jdbcClient.sql("UPDATE users SET account_locked_until = :lockedUntil, updated_at = NOW() WHERE id = :id")
                .param("lockedUntil", Timestamp.from(lockedUntil))
                .param("id", userId)
                .update();
    }

    @Override
    public void updateLastLogin(Long userId, Instant loginTime, String ipAddress) {
        jdbcClient.sql("UPDATE users SET last_login_at = :loginTime, last_login_ip = :ip, updated_at = NOW() WHERE id = :id")
                .param("loginTime", Timestamp.from(loginTime))
                .param("ip", ipAddress)
                .param("id", userId)
                .update();
    }

    @Override
    public boolean existsByUsername(String username) {
        Long count = jdbcClient.sql("SELECT COUNT(*) FROM users WHERE username = :username")
                .param("username", username)
                .query(Long.class)
                .single();
        return count > 0;
    }

    @Override
    public boolean existsByEmail(String email) {
        Long count = jdbcClient.sql("SELECT COUNT(*) FROM users WHERE email = :email")
                .param("email", email)
                .query(Long.class)
                .single();
        return count > 0;
    }

    @Override
    public boolean existsByErpNo(String erpNo) {
        Long count = jdbcClient.sql("SELECT COUNT(*) FROM users WHERE erp_no = :erpNo")
                .param("erpNo", erpNo)
                .query(Long.class)
                .single();
        return count > 0;
    }

    @Override
    public boolean existsByCnic(String cnic) {
        Long count = jdbcClient.sql("SELECT COUNT(*) FROM users WHERE cnic = :cnic")
                .param("cnic", cnic)
                .query(Long.class)
                .single();
        return count > 0;
    }

    private Timestamp toTimestamp(Instant instant) {
        return instant != null ? Timestamp.from(instant) : null;
    }
}
