package com.lokmit.foundation.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

/**
 * Standard REST API response envelope wrapping every business endpoint response.
 *
 * <p>Success responses populate {@code data}; failure responses populate
 * {@code errors}. The {@code timestamp} is set at construction time.</p>
 *
 * @param <T> payload type carried in {@code data}
 */
@Getter
public class ApiResponse<T> {

    private final boolean success;
    private final String message;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final T data;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final List<ApiError> errors;

    private final Instant timestamp;

    private ApiResponse(boolean success, String message, T data, List<ApiError> errors) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.errors = errors;
        this.timestamp = Instant.now();
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "OK", data, null);
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(true, message, data, null);
    }

    public static <T> ApiResponse<T> error(String message, List<ApiError> errors) {
        return new ApiResponse<>(false, message, null, errors);
    }

    public static <T> ApiResponse<T> error(String message, ApiError error) {
        return new ApiResponse<>(false, message, null, List.of(error));
    }
}