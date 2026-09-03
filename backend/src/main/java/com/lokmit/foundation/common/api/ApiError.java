package com.lokmit.foundation.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

/**
 * Single structured error entry inside an {@link ApiResponse}.
 *
 * <p>{@code field} is present only for validation/binding errors and is
 * otherwise omitted from the JSON payload.</p>
 */
@Getter
public class ApiError {

    private final String code;
    private final String message;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final String field;

    private ApiError(String code, String message, String field) {
        this.code = code;
        this.message = message;
        this.field = field;
    }

    public static ApiError of(String code, String message) {
        return new ApiError(code, message, null);
    }

    public static ApiError field(String code, String message, String field) {
        return new ApiError(code, message, field);
    }
}