package com.lokmit.foundation.common.exception;

import jakarta.validation.constraints.NotBlank;

/**
 * Minimal payload for binding-validation tests. Not part of production code.
 */
public class TestPayload {

    @NotBlank(message = "name must not be blank")
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
