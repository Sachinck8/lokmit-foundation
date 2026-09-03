package com.lokmit.foundation.common.api;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseTest {

    @Test
    void success_shouldSetSuccessStateAndData() {
        ApiResponse<String> response = ApiResponse.success("hello");

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isEqualTo("hello");
        assertThat(response.getErrors()).isNull();
        assertThat(response.getTimestamp()).isNotNull();
    }

    @Test
    void successWithMessage_shouldSetMessage() {
        ApiResponse<String> response = ApiResponse.success("hello", "Widget created");

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).isEqualTo("Widget created");
        assertThat(response.getData()).isEqualTo("hello");
    }

    @Test
    void errorWithList_shouldSetFailureStateAndErrors() {
        ApiError first = ApiError.field(ErrorCodes.VALIDATION, "must not be blank", "name");
        ApiError second = ApiError.of(ErrorCodes.BAD_REQUEST, "bad input");

        ApiResponse<Void> response = ApiResponse.error("Validation failed", List.of(first, second));

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getData()).isNull();
        assertThat(response.getErrors()).hasSize(2);
        assertThat(response.getErrors().get(0).getCode()).isEqualTo(ErrorCodes.VALIDATION);
        assertThat(response.getErrors().get(0).getField()).isEqualTo("name");
        assertThat(response.getErrors().get(1).getCode()).isEqualTo(ErrorCodes.BAD_REQUEST);
        assertThat(response.getErrors().get(1).getField()).isNull();
    }

    @Test
    void errorWithSingle_shouldWrapOneError() {
        ApiResponse<Void> response = ApiResponse.error(
                "Widget not found",
                ApiError.of(ErrorCodes.NOT_FOUND, "Widget not found"));

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getErrors()).hasSize(1);
        assertThat(response.getErrors().get(0).getCode()).isEqualTo(ErrorCodes.NOT_FOUND);
    }
}