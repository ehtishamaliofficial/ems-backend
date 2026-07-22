package com.nadra.ems.common.exception;

/**
 * Thrown when authentication fails (bad credentials, invalid token, etc.).
 */
public class AuthenticationFailedException extends RuntimeException {

    public AuthenticationFailedException(String message) {
        super(message);
    }

    public AuthenticationFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
