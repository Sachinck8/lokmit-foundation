package com.lokmit.foundation.common.api;

/**
 * Canonical machine-readable error codes used across the API.
 */
public final class ErrorCodes {

    // Existing error codes
    public static final String NOT_FOUND = "NOT_FOUND";
    public static final String BAD_REQUEST = "BAD_REQUEST";
    public static final String VALIDATION = "VALIDATION";
    public static final String CONFLICT = "CONFLICT";
    public static final String METHOD_NOT_ALLOWED = "METHOD_NOT_ALLOWED";
    public static final String UNSUPPORTED_MEDIA_TYPE = "UNSUPPORTED_MEDIA_TYPE";
    public static final String INTERNAL_ERROR = "INTERNAL_ERROR";

    // Phase 4: Authentication & Authorization error codes
    public static final String INVALID_CREDENTIALS = "INVALID_CREDENTIALS";
    public static final String USER_LOCKED = "USER_LOCKED";
    public static final String USER_SUSPENDED = "USER_SUSPENDED";
    public static final String USER_DELETED = "USER_DELETED";
    public static final String INVALID_TOKEN = "INVALID_TOKEN";
    public static final String EXPIRED_TOKEN = "EXPIRED_TOKEN";
    public static final String INVALID_REFRESH_TOKEN = "INVALID_REFRESH_TOKEN";
    public static final String REVOKED_REFRESH_TOKEN = "REVOKED_REFRESH_TOKEN";
    public static final String UNAUTHORIZED = "UNAUTHORIZED";
    public static final String FORBIDDEN = "FORBIDDEN";

    private ErrorCodes() {
        throw new AssertionError("Utility class must not be instantiated.");
    }
}