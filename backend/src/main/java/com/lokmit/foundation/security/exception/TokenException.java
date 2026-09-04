package com.lokmit.foundation.security.exception;

/**
 * Raised when a JWT or refresh token is invalid, expired, or revoked.
 */
public class TokenException extends RuntimeException {

    public TokenException(String message) {
        super(message);
    }
}