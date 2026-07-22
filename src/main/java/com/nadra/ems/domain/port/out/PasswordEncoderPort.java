package com.nadra.ems.domain.port.out;

/**
 * Driven port (SPI) — password encoding/verification contract.
 * Decouples the domain from the concrete hashing algorithm (BCrypt).
 */
public interface PasswordEncoderPort {

    /**
     * Hashes a raw password.
     */
    String encode(String rawPassword);

    /**
     * Verifies a raw password against a stored hash.
     */
    boolean matches(String rawPassword, String encodedPassword);
}
