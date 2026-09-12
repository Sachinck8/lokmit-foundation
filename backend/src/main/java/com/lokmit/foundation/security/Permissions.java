package com.lokmit.foundation.security;

/**
 * Canonical permission authority codes enforced by Spring Security method
 * authorization ({@code @PreAuthorize}).
 *
 * <p>These codes match the permissions seeded in
 * {@code V2__identity_schema.sql} (table {@code permissions}) one-to-one.
 * They are the single source of truth for endpoint authorization: the
 * database grants them to roles via {@code role_permissions}, and
 * {@link com.lokmit.foundation.security.service.CustomUserDetailsService}
 * materializes each user's granted permission codes as Spring Security
 * authorities on every request from the database.</p>
 *
 * <p><strong>Usage for all future Admin APIs:</strong></p>
 * <pre>{@code
 * @GetMapping("/api/v1/admin/...")
 * @PreAuthorize("hasAuthority('" + Permissions.USERS_MANAGE + "')")
 * public ResponseEntity<ApiResponse<...>> endpoint() { ... }
 * }</pre>
 *
 * <p><strong>Rules:</strong></p>
 * <ul>
 *   <li>Never authorize on {@code hasRole(...)} alone for management
 *       endpoints — permissions are the stable contract; role membership is
 *       administrative data that can be re-granted without code changes.</li>
 *   <li>Never accept role or permission identifiers from the client as
 *       authorization input; authorities come only from the validated
 *       security context (database-backed, per request).</li>
 *   <li>When a new permission is genuinely required, add it in a new Flyway
 *       migration and grant it to specific roles there — do not invent
 *       speculative codes here.</li>
 * </ul>
 *
 * <p><strong>Role → permission summary (V2 seed data):</strong></p>
 * <ul>
 *   <li>SUPER_ADMIN — all permissions below</li>
 *   <li>ADMIN — content:manage, content:publish, downloads:manage,
 *       messages:manage, jobs:manage, settings:manage</li>
 *   <li>EDITOR — content:manage, downloads:manage</li>
 *   <li>MODERATOR — jobs:moderate, messages:manage</li>
 *   <li>CANDIDATE, EMPLOYER, CLIENT — none (normal platform users; never
 *       administrative access)</li>
 * </ul>
 */
public final class Permissions {

    // Seeded in V2__identity_schema.sql — keep codes identical to the seed.

    /** Create and edit website content. */
    public static final String CONTENT_MANAGE = "content:manage";

    /** Publish and unpublish website content. */
    public static final String CONTENT_PUBLISH = "content:publish";

    /** Manage downloadable resources. */
    public static final String DOWNLOADS_MANAGE = "downloads:manage";

    /** View and manage contact messages. */
    public static final String MESSAGES_MANAGE = "messages:manage";

    /** Manage users, roles and permissions (SUPER_ADMIN only). */
    public static final String USERS_MANAGE = "users:manage";

    /** Create, edit and publish job postings. */
    public static final String JOBS_MANAGE = "jobs:manage";

    /** Review and moderate jobs and applications. */
    public static final String JOBS_MODERATE = "jobs:moderate";

    /** Manage site settings and configuration. */
    public static final String SETTINGS_MANAGE = "settings:manage";

    private Permissions() {
        throw new AssertionError("Utility class must not be instantiated.");
    }
}
