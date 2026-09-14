package com.lokmit.foundation.common.constants;

/**
 * Central home for URL path constants so controllers never repeat string literals.
 */
public final class ApiPaths {

    /** Versioned API root. All future endpoints are created under this prefix. */
    public static final String API_V1 = "/api/v1";

    /** Public health check (unauthenticated). */
    public static final String HEALTH = API_V1 + "/health";

    /** Authentication endpoints. */
    public static final String AUTH = API_V1 + "/auth";
    public static final String AUTH_LOGIN = AUTH + "/login";
    public static final String AUTH_REFRESH = AUTH + "/refresh";
    public static final String AUTH_LOGOUT = AUTH + "/logout";
    public static final String AUTH_ME = AUTH + "/me";

    /** Public contact enquiries (submission only; management is authenticated). */
    public static final String CONTACT_MESSAGES = API_V1 + "/contact-messages";

    /** Admin dashboard read APIs (A2) — guarded by the dashboard:view permission. */
    public static final String ADMIN_DASHBOARD = API_V1 + "/admin/dashboard";

    /** Admin user management APIs (A3) — guarded by the users:manage permission. */
    public static final String ADMIN_USERS = API_V1 + "/admin/users";

    /** Admin CMS management namespace (A4) — guarded by CMS permissions. */
    public static final String ADMIN_CMS = API_V1 + "/admin/cms";
    public static final String ADMIN_CMS_SITE_SETTINGS = ADMIN_CMS + "/site-settings";
    public static final String ADMIN_CMS_WEBSITE_CONTENT = ADMIN_CMS + "/website-content";
    public static final String ADMIN_CMS_SEO_METADATA = ADMIN_CMS + "/seo-metadata";

    /** Admin services & expertise management APIs (A5) — guarded by services:manage. */
    public static final String ADMIN_SERVICE_CATEGORIES = API_V1 + "/admin/service-categories";
    public static final String ADMIN_SERVICES = API_V1 + "/admin/services";
    public static final String ADMIN_EXPERTISE_AREAS = API_V1 + "/admin/expertise-areas";

    /** Admin projects management APIs (A6) — guarded by projects:manage. */
    public static final String ADMIN_PROJECT_CATEGORIES = API_V1 + "/admin/project-categories";
    public static final String ADMIN_PROJECTS = API_V1 + "/admin/projects";

    private ApiPaths() {
        throw new AssertionError("Utility class must not be instantiated.");
    }
}
