package com.lokmit.foundation.security.exception;

/**
 * Raised when authentication fails due to invalid credentials.
 * Mapped to HTTP 401 with a generic error message that does not reveal whether the email exists.
 */
public class AuthenticationFailedException extends RuntimeException {

    public AuthenticationFailedException(String message) {
        super(message);
    }
}