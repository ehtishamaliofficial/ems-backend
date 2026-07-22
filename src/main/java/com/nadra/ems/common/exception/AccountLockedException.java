package com.nadra.ems.common.exception;

import lombok.Getter;

/**
 * Thrown when an account is temporarily locked due to excessive failed login attempts.
 */
@Getter
public class AccountLockedException extends RuntimeException {

    private final java.time.Instant lockedUntil;

    public AccountLockedException(String message, java.time.Instant lockedUntil) {
        super(message);
        this.lockedUntil = lockedUntil;
    }

}
