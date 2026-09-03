package com.lokmit.foundation.common.api;

/**
 * Canonical machine-readable error codes used across the API.
 */
public final class ErrorCodes {

    public static final String NOT_FOUND = "NOT_FOUND";
    public static final String BAD_REQUEST = "BAD_REQUEST";
    public static final String VALIDATION = "VALIDATION";
    public static final String CONFLICT = "CONFLICT";
    public static final String METHOD_NOT_ALLOWED = "METHOD_NOT_ALLOWED";
    public static final String UNSUPPORTED_MEDIA_TYPE = "UNSUPPORTED_MEDIA_TYPE";
    public static final String INTERNAL_ERROR = "INTERNAL_ERROR";

    private ErrorCodes() {
        throw new AssertionError("Utility class must not be instantiated.");
    }
}