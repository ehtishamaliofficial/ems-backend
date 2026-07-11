package com.nadra.ems.auth.adapter.out.persistence.exception;

import com.nadra.ems.common.exception.DuplicateResourceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Translates JDBC/PostgreSQL exceptions into domain-specific exceptions.
 * <p>
 * PostgreSQL error codes handled:
 * <ul>
 *     <li>{@code 23505} — unique_violation → {@link DuplicateResourceException}</li>
 *     <li>{@code 23503} — foreign_key_violation → meaningful message</li>
 *     <li>{@code 23502} — not_null_violation → validation error</li>
 * </ul>
 */
@Component
public class DatabaseExceptionTranslator {

    private static final Logger log = LoggerFactory.getLogger(DatabaseExceptionTranslator.class);

    // Pattern to extract constraint name from PostgreSQL error messages
    private static final Pattern CONSTRAINT_PATTERN =
            Pattern.compile("constraint \"(\\w+)\"", Pattern.CASE_INSENSITIVE);

    /**
     * Translates a {@link DuplicateKeyException} into a domain-specific
     * {@link DuplicateResourceException} with a meaningful message.
     *
     * @param ex the JDBC exception
     * @return a domain exception with user-friendly message
     */
    public DuplicateResourceException translateDuplicateKey(DuplicateKeyException ex) {
        String message = ex.getMostSpecificCause().getMessage();
        log.debug("Translating duplicate key exception: {}", message);

        String constraintName = extractConstraintName(message);

        return switch (constraintName) {
            case "users_erp_no_key" ->
                    new DuplicateResourceException("User", "erpNo", extractValue(message));
            case "users_username_key" ->
                    new DuplicateResourceException("User", "username", extractValue(message));
            case "users_email_key" ->
                    new DuplicateResourceException("User", "email", extractValue(message));
            case "users_cnic_key" ->
                    new DuplicateResourceException("User", "cnic", extractValue(message));
            default ->
                    new DuplicateResourceException("Resource", "field", "duplicate value");
        };
    }

    /**
     * Checks if a {@link DataAccessException} is a duplicate key error and translates it.
     * Returns null if it's not a duplicate key exception.
     */
    public DuplicateResourceException translateIfDuplicate(DataAccessException ex) {
        if (ex instanceof DuplicateKeyException dke) {
            return translateDuplicateKey(dke);
        }
        return null;
    }

    private String extractConstraintName(String message) {
        Matcher matcher = CONSTRAINT_PATTERN.matcher(message);
        return matcher.find() ? matcher.group(1) : "unknown";
    }

    private String extractValue(String message) {
        // PostgreSQL format: Key (column)=(value) already exists
        Pattern valuePattern = Pattern.compile("\\(([^)]+)\\)=\\(([^)]+)\\)");
        Matcher matcher = valuePattern.matcher(message);
        return matcher.find() ? matcher.group(2) : "unknown";
    }
}
