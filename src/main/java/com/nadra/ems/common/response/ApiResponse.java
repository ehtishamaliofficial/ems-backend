package com.nadra.ems.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

import java.time.Instant;

/**
 * Generic API response wrapper providing a consistent response structure
 * across all endpoints.
 *
 * @param <T> the type of the response data payload
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final boolean success;
    private final int statusCode;
    private final String message;
    private final T data;
    private final String timestamp;
    private final String path;

    private ApiResponse(boolean success, int statusCode, String message, T data, String path) {
        this.success = success;
        this.statusCode = statusCode;
        this.message = message;
        this.data = data;
        this.timestamp = Instant.now().toString();
        this.path = path;
    }

    // ── Static factory methods ──────────────────────────────────────────────

    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(true, 200, message, data, null);
    }

    public static <T> ApiResponse<T> success(T data) {
        return success(data, "Operation successful");
    }

    public static <T> ApiResponse<T> created(T data, String message) {
        return new ApiResponse<>(true, 201, message, data, null);
    }

    public static <T> ApiResponse<T> created(T data) {
        return created(data, "Resource created successfully");
    }

    public static <T> ApiResponse<T> error(int statusCode, String message, String path) {
        return new ApiResponse<>(false, statusCode, message, null, path);
    }

    public static <T> ApiResponse<T> error(int statusCode, String message) {
        return error(statusCode, message, null);
    }

    public static <T> ApiResponse<T> errorWithData(int statusCode, String message, T data, String path) {
        return new ApiResponse<>(false, statusCode, message, data, path);
    }
}
