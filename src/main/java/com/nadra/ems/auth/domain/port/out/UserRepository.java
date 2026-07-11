package com.nadra.ems.auth.domain.port.out;

import com.nadra.ems.auth.domain.model.User;

import java.time.Instant;
import java.util.Optional;

/**
 * Driven port (SPI) — repository contract for User persistence.
 * Implemented by the JDBC adapter in the infrastructure layer.
 */
public interface UserRepository {

    /**
     * Persists a new user and returns the user with the generated ID.
     */
    User save(User user);

    /**
     * Finds a user by their auto-generated surrogate ID.
     */
    Optional<User> findById(Long id);

    /**
     * Finds a user by their business ERP number.
     */
    Optional<User> findByErpNo(String erpNo);

    /**
     * Finds a user by username.
     */
    Optional<User> findByUsername(String username);

    /**
     * Finds a user by email address.
     */
    Optional<User> findByEmail(String email);

    /**
     * Finds a user by CNIC (national ID).
     */
    Optional<User> findByCnic(String cnic);

    /**
     * Updates an existing user record.
     */
    void update(User user);

    /**
     * Updates only the password hash for a user.
     */
    void updatePassword(Long userId, String passwordHash);

    /**
     * Updates the TOTP secret and enabled flag for 2FA.
     */
    void updateTwoFactorSecret(Long userId, String secret, boolean enabled);

    /**
     * Increments the failed login attempt counter.
     */
    void incrementFailedAttempts(Long userId);

    /**
     * Resets failed login attempts back to zero.
     */
    void resetFailedAttempts(Long userId);

    /**
     * Locks the account until the given timestamp.
     */
    void lockAccount(Long userId, Instant lockedUntil);

    /**
     * Records the last login timestamp and IP address.
     */
    void updateLastLogin(Long userId, Instant loginTime, String ipAddress);

    /**
     * Checks whether a username already exists.
     */
    boolean existsByUsername(String username);

    /**
     * Checks whether an email already exists.
     */
    boolean existsByEmail(String email);

    /**
     * Checks whether an ERP number already exists.
     */
    boolean existsByErpNo(String erpNo);

    /**
     * Checks whether a CNIC already exists.
     */
    boolean existsByCnic(String cnic);
}
