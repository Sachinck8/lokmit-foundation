package com.lokmit.foundation.common.exception;

/**
 * Raised when an operation conflicts with the current resource state
 * (for example a uniqueness violation). Mapped to HTTP 409.
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}