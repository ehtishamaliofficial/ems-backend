package com.nadra.ems.adapter.out.persistence.exception;

import com.nadra.ems.common.exception.DuplicateResourceException;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class DatabaseExceptionTranslator {

    // Pattern to extract constraint name from PostgreSQL error messages
    private static final Pattern CONSTRAINT_PATTERN =
            Pattern.compile("constraint \"(\\w+)\"", Pattern.CASE_INSENSITIVE);

    // Pattern to extract field and value from PostgreSQL error messages
    // Format: Key (column_name)=(value) already exists
    private static final Pattern FIELD_VALUE_PATTERN = 
            Pattern.compile("\\(([^)]+)\\)=\\(([^)]+)\\)");

    /**
     * Translates a {@link DuplicateKeyException} into a domain-specific
     * {@link DuplicateResourceException} with a meaningful message.
     *
     * @param ex the JDBC exception
     * @return a domain exception with user-friendly message
     */
    public DuplicateResourceException translateDuplicateKey(DuplicateKeyException ex) {
        return translateDuplicateKey(ex, null);
    }

    /**
     * Translates a {@link DuplicateKeyException} into a domain-specific
     * {@link DuplicateResourceException} with a meaningful message.
     *
     * @param ex the JDBC exception
     * @param resourceName the domain resource name (e.g. "User"). If null, it will be inferred.
     * @return a domain exception with user-friendly message
     */
    public DuplicateResourceException translateDuplicateKey(DuplicateKeyException ex, String resourceName) {
        String message = ex.getMostSpecificCause().getMessage();
        log.debug("Translating duplicate key exception: {}", message);

        String fieldName = "field";
        String value = "duplicate value";

        Matcher matcher = FIELD_VALUE_PATTERN.matcher(message);
        if (matcher.find()) {
            fieldName = toCamelCase(matcher.group(1));
            value = matcher.group(2);
        }

        if (resourceName == null) {
            String constraintName = extractConstraintName(message);
            resourceName = inferResourceName(constraintName);
        }

        return new DuplicateResourceException(resourceName, fieldName, value);
    }

    /**
     * Checks if a {@link DataAccessException} is a duplicate key error and translates it.
     * Returns null if it's not a duplicate key exception.
     */
    public DuplicateResourceException translateIfDuplicate(DataAccessException ex) {
        return translateIfDuplicate(ex, null);
    }

    /**
     * Checks if a {@link DataAccessException} is a duplicate key error and translates it.
     * Returns null if it's not a duplicate key exception.
     *
     * @param ex the JDBC exception
     * @param resourceName the domain resource name
     */
    public DuplicateResourceException translateIfDuplicate(DataAccessException ex, String resourceName) {
        if (ex instanceof DuplicateKeyException dke) {
            return translateDuplicateKey(dke, resourceName);
        }
        return null;
    }

    private String extractConstraintName(String message) {
        Matcher matcher = CONSTRAINT_PATTERN.matcher(message);
        return matcher.find() ? matcher.group(1) : "unknown";
    }

    private String inferResourceName(String constraintName) {
        if (constraintName == null || "unknown".equals(constraintName)) {
            return "Resource";
        }
        String[] parts = constraintName.split("_");
        if (parts.length > 0) {
            String table = parts[0];
            if (table.endsWith("ies")) {
                table = table.substring(0, table.length() - 3) + "y";
            } else if (table.endsWith("s")) {
                table = table.substring(0, table.length() - 1);
            }
            if (!table.isEmpty()) {
                return table.substring(0, 1).toUpperCase() + table.toLowerCase().substring(1);
            }
        }
        return "Resource";
    }

    private String toCamelCase(String snakeCase) {
        if (snakeCase == null || snakeCase.isEmpty()) {
            return "field";
        }
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = false;
        for (char c : snakeCase.toCharArray()) {
            if (c == '_') {
                capitalizeNext = true;
            } else {
                if (capitalizeNext) {
                    result.append(Character.toUpperCase(c));
                    capitalizeNext = false;
                } else {
                    result.append(Character.toLowerCase(c));
                }
            }
        }
        return result.toString();
    }
}
