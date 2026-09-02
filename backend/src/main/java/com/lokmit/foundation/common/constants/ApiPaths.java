package com.lokmit.foundation.common.constants;

/**
 * Central home for URL path constants so controllers never repeat string literals.
 */
public final class ApiPaths {

    /** Versioned API root. All future endpoints are created under this prefix. */
    public static final String API_V1 = "/api/v1";

    /** Public health check (unauthenticated). */
    public static final String HEALTH = API_V1 + "/health";

    private ApiPaths() {
        throw new AssertionError("Utility class must not be instantiated.");
    }
}