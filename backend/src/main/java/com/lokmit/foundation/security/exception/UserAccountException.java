package com.lokmit.foundation.security.exception;

/**
 * Raised when a user account is locked, suspended, or deleted.
 * The specific status is carried to allow appropriate error responses.
 */
public class UserAccountException extends RuntimeException {

    private final String status;

    public UserAccountException(String message, String status) {
        super(message);
        this.status = status;
    }

    public String getStatus() {
        return status;
    }
}