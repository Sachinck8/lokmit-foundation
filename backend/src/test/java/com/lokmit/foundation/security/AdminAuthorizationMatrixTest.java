package com.lokmit.foundation.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lokmit.foundation.common.constants.ApiPaths;
import com.lokmit.foundation.security.config.CorsConfig;
import com.lokmit.foundation.security.config.JwtKeyConfig;
import com.lokmit.foundation.security.config.SecurityConfig;
import com.lokmit.foundation.security.controller.AuthController;
import com.lokmit.foundation.security.dto.AuthResponse;
import com.lokmit.foundation.security.dto.LoginRequest;
import com.lokmit.foundation.security.dto.RefreshTokenRequest;
import com.lokmit.foundation.security.entity.Permission;
import com.lokmit.foundation.security.entity.Role;
import com.lokmit.foundation.security.entity.User;
import com.lokmit.foundation.security.repository.UserRepository;
import com.lokmit.foundation.security.service.AuthService;
import com.lokmit.foundation.security.service.CustomUserDetailsService;
import com.lokmit.foundation.security.service.JwtTokenProvider;
import com.lokmit.foundation.security.util.SecurityUtils;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A1 security test matrix, exercised through the REAL Spring Security filter
 * chain: the imported {@link SecurityConfig} brings the actual
 * {@code JwtAuthenticationFilter}, the real {@link JwtTokenProvider} (tokens
 * below are genuinely signed and verified), the real
 * {@link CustomUserDetailsService} (the user repository is mocked at the
 * persistence boundary, so no database is required), method security, and the
 * standard 401/403 error envelopes.
 *
 * <p>Coverage (task A1, section 10):</p>
 * <ul>
 *   <li>A anonymous → 401 on protected endpoints</li>
 *   <li>B authenticated normal user (no permissions) → 403</li>
 *   <li>C MODERATOR allowed on moderator permissions, 403 on admin-only</li>
 *   <li>D ADMIN allowed on admin permissions, 403 on SUPER_ADMIN-only</li>
 *   <li>E SUPER_ADMIN allowed everywhere</li>
 *   <li>F invalid JWT → 401; tampered signature → 401</li>
 *   <li>G expired JWT → 401</li>
 *   <li>H LOCKED / SUSPENDED / DELETED account with a valid JWT → rejected</li>
 *   <li>I missing permission → 403</li>
 *   <li>J {@code /api/v1/auth/me}: anonymous → 401, authenticated → 200</li>
 *   <li>K public endpoints stay public (health, login, refresh) and logout
 *       behavior stays compatible</li>
 * </ul>
 *
 * <p>The test controller {@link AuthorizationBaselineController} maps one
 * seeded permission per endpoint so the matrix matches the real V2 seed
 * semantics (SUPER_ADMIN = all, ADMIN = six, MODERATOR = jobs:moderate +
 * messages:manage, CANDIDATE = none).</p>
 */
@WebMvcTest(controllers = {
        AuthorizationBaselineController.class,
        AuthController.class,
        com.lokmit.foundation.common.health.HealthController.class})
@Import({SecurityConfig.class, CorsConfig.class, JwtKeyConfig.class,
        JwtTokenProvider.class, CustomUserDetailsService.class, SecurityUtils.class})
@AutoConfigureMockMvc
class AdminAuthorizationMatrixTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private SecretKey signingKey;

    @MockitoBean
    private UserRepository userRepository;

    // AuthController dependency; the security matrix does not exercise its logic.
    @MockitoBean
    private AuthService authService;

    private static final String BASELINE = "/api/v1/security-baseline";

    // ------------------------------------------------------------------
    // DB-backed user fixtures (mirror the V2 seed permission grants)
    // ------------------------------------------------------------------

    @BeforeEach
    void resetRepositoryStub() {
        org.mockito.Mockito.reset(userRepository);
    }

    private User dbUser(long id, String email, String status, Role... roles) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setPasswordHash("$2a$10$storedhashnotusedbyjwtfilter012345678901234567890123");
        user.setFullName("Test User");
        user.setUserType("STAFF");
        user.setStatus(status);
        user.setEmailVerified(true);
        user.setCreatedAt(OffsetDateTime.now());
        user.setUpdatedAt(OffsetDateTime.now());
        user.setRoles(Set.of(roles));
        return user;
    }

    private Role role(String code, String... permissionCodes) {
        Role role = new Role();
        role.setCode(code);
        role.setPermissions(Set.of(permissionCodes).stream().map(code0 -> {
            Permission permission = new Permission();
            permission.setCode(code0);
            return permission;
        }).collect(Collectors.toSet()));
        return role;
    }

    private void givenDbUser(User user) {
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
    }

    private ResultActions baselineGet(String path, String bearerToken) throws Exception {
        var request = get(path);
        if (bearerToken != null) {
            request.header("Authorization", "Bearer " + bearerToken);
        }
        return mockMvc.perform(request);
    }

    private static final String CANDIDATE_EMAIL = "candidate@lokmitfoundation.org";
    private static final String MODERATOR_EMAIL = "moderator@lokmitfoundation.org";
    private static final String ADMIN_EMAIL = "admin@lokmitfoundation.org";
    private static final String SUPER_ADMIN_EMAIL = "superadmin@lokmitfoundation.org";
    private static final String LOCKED_EMAIL = "locked@lokmitfoundation.org";
    private static final String SUSPENDED_EMAIL = "suspended@lokmitfoundation.org";
    private static final String DELETED_EMAIL = "deleted@lokmitfoundation.org";

    // ------------------------------------------------------------------
    // A. Anonymous access
    // ------------------------------------------------------------------

    @Test
    @DisplayName("A: anonymous request to a protected endpoint is rejected with 401 envelope")
    void anonymousProtectedEndpointIs401() throws Exception {
        mockMvc.perform(get(BASELINE + "/users-manage"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errors[0].code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("A: anonymous request with a non-Bearer authorization header is rejected with 401")
    void nonBearerAuthorizationIs401() throws Exception {
        mockMvc.perform(get(BASELINE + "/users-manage").header("Authorization", "Basic dXNlcjpwYXNz"))
                .andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------
    // B + I. Authenticated normal user without the required permission
    // ------------------------------------------------------------------

    @Test
    @DisplayName("B: CANDIDATE (no permissions) gets 403 on an admin permission endpoint")
    void normalUserGets403OnAdminEndpoint() throws Exception {
        givenDbUser(dbUser(2L, CANDIDATE_EMAIL, "ACTIVE",
                role("CANDIDATE")));

        baselineGet(BASELINE + "/users-manage",
                jwtTokenProvider.generateAccessToken(2L, CANDIDATE_EMAIL, List.of("CANDIDATE")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errors[0].code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("I: CANDIDATE gets 403 on messages:manage despite being authenticated")
    void missingPermissionIs403() throws Exception {
        givenDbUser(dbUser(2L, CANDIDATE_EMAIL, "ACTIVE",
                role("CANDIDATE")));

        baselineGet(BASELINE + "/messages-manage",
                jwtTokenProvider.generateAccessToken(2L, CANDIDATE_EMAIL, List.of("CANDIDATE")))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------
    // IDOR / claim-trust: client-supplied or JWT-claim roles are never used
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Security context authority: a roles claim of SUPER_ADMIN cannot elevate a CANDIDATE (DB is authoritative)")
    void jwtRolesClaimCannotElevatePermissions() throws Exception {
        givenDbUser(dbUser(2L, CANDIDATE_EMAIL, "ACTIVE",
                role("CANDIDATE")));

        // Token carries a SUPER_ADMIN roles claim, but the DB says CANDIDATE.
        String spoofed = jwtTokenProvider.generateAccessToken(
                2L, CANDIDATE_EMAIL, List.of("SUPER_ADMIN"));

        baselineGet(BASELINE + "/users-manage", spoofed)
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------
    // C. MODERATOR — limited management authority
    // ------------------------------------------------------------------

    @Test
    @DisplayName("C: MODERATOR may use moderator-permitted endpoints")
    void moderatorAllowedOnModeratorPermissions() throws Exception {
        givenDbUser(dbUser(3L, MODERATOR_EMAIL, "ACTIVE",
                role("MODERATOR", "jobs:moderate", "messages:manage")));
        String token = jwtTokenProvider.generateAccessToken(3L, MODERATOR_EMAIL, List.of("MODERATOR"));

        baselineGet(BASELINE + "/messages-manage", token).andExpect(status().isOk());
        baselineGet(BASELINE + "/jobs-moderate", token).andExpect(status().isOk());
    }

    @Test
    @DisplayName("C: MODERATOR is denied admin-only endpoints")
    void moderatorDeniedAdminOnlyPermissions() throws Exception {
        givenDbUser(dbUser(3L, MODERATOR_EMAIL, "ACTIVE",
                role("MODERATOR", "jobs:moderate", "messages:manage")));
        String token = jwtTokenProvider.generateAccessToken(3L, MODERATOR_EMAIL, List.of("MODERATOR"));

        baselineGet(BASELINE + "/users-manage", token).andExpect(status().isForbidden());
        baselineGet(BASELINE + "/settings-manage", token).andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------
    // D. ADMIN — normal administrative authority
    // ------------------------------------------------------------------

    @Test
    @DisplayName("D: ADMIN may use admin-permitted endpoints")
    void adminAllowedOnAdminPermissions() throws Exception {
        givenDbUser(dbUser(4L, ADMIN_EMAIL, "ACTIVE",
                role("ADMIN", "content:manage", "content:publish", "downloads:manage",
                        "messages:manage", "jobs:manage", "settings:manage")));
        String token = jwtTokenProvider.generateAccessToken(4L, ADMIN_EMAIL, List.of("ADMIN"));

        baselineGet(BASELINE + "/settings-manage", token).andExpect(status().isOk());
        baselineGet(BASELINE + "/messages-manage", token).andExpect(status().isOk());
    }

    @Test
    @DisplayName("D: ADMIN is denied users:manage (SUPER_ADMIN-only permission)")
    void adminDeniedUsersManage() throws Exception {
        givenDbUser(dbUser(4L, ADMIN_EMAIL, "ACTIVE",
                role("ADMIN", "content:manage", "content:publish", "downloads:manage",
                        "messages:manage", "jobs:manage", "settings:manage")));
        String token = jwtTokenProvider.generateAccessToken(4L, ADMIN_EMAIL, List.of("ADMIN"));

        baselineGet(BASELINE + "/users-manage", token).andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------
    // E. SUPER_ADMIN — highest authority
    // ------------------------------------------------------------------

    @Test
    @DisplayName("E: SUPER_ADMIN is allowed on every seeded-permission endpoint")
    void superAdminAllowedEverywhere() throws Exception {
        givenDbUser(dbUser(5L, SUPER_ADMIN_EMAIL, "ACTIVE",
                role("SUPER_ADMIN", "content:manage", "content:publish", "downloads:manage",
                        "messages:manage", "users:manage", "jobs:manage", "jobs:moderate",
                        "settings:manage")));
        String token = jwtTokenProvider.generateAccessToken(5L, SUPER_ADMIN_EMAIL, List.of("SUPER_ADMIN"));

        baselineGet(BASELINE + "/users-manage", token).andExpect(status().isOk());
        baselineGet(BASELINE + "/messages-manage", token).andExpect(status().isOk());
        baselineGet(BASELINE + "/jobs-moderate", token).andExpect(status().isOk());
        baselineGet(BASELINE + "/settings-manage", token).andExpect(status().isOk());
    }

    // ------------------------------------------------------------------
    // F + G. Invalid, tampered, expired JWTs
    // ------------------------------------------------------------------

    @Test
    @DisplayName("F: structurally invalid JWT is rejected with 401")
    void invalidJwtIs401() throws Exception {
        baselineGet(BASELINE + "/users-manage", "this.is.not.a.jwt")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errors[0].code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("F: JWT signed with the wrong key is rejected with 401")
    void tamperedSignatureIs401() throws Exception {
        SecretKey attackerKey = Jwts.SIG.HS256.key().build();
        String forged = Jwts.builder()
                .subject("2")
                .claim("email", CANDIDATE_EMAIL)
                .claim("roles", List.of("SUPER_ADMIN"))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 600_000))
                .signWith(attackerKey)
                .compact();

        baselineGet(BASELINE + "/users-manage", forged)
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("G: expired JWT is rejected with 401")
    void expiredJwtIs401() throws Exception {
        String expired = Jwts.builder()
                .subject("1")
                .claim("email", ADMIN_EMAIL)
                .issuedAt(new Date(System.currentTimeMillis() - 120_000))
                .expiration(new Date(System.currentTimeMillis() - 60_000))
                .signWith(signingKey)
                .compact();

        baselineGet(BASELINE + "/users-manage", expired)
                .andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------
    // H. Account status enforcement (valid JWT, deactivated account)
    // ------------------------------------------------------------------

    @Test
    @DisplayName("H: LOCKED account with a valid JWT is rejected (fail closed)")
    void lockedAccountWithValidJwtIsRejected() throws Exception {
        givenDbUser(dbUser(6L, LOCKED_EMAIL, "LOCKED", role("ADMIN",
                "content:manage", "content:publish", "downloads:manage",
                "messages:manage", "jobs:manage", "settings:manage")));
        String token = jwtTokenProvider.generateAccessToken(6L, LOCKED_EMAIL, List.of("ADMIN"));

        baselineGet(BASELINE + "/settings-manage", token)
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("H: SUSPENDED account with a valid JWT is rejected (fail closed)")
    void suspendedAccountWithValidJwtIsRejected() throws Exception {
        givenDbUser(dbUser(7L, SUSPENDED_EMAIL, "SUSPENDED", role("MODERATOR", "jobs:moderate")));
        String token = jwtTokenProvider.generateAccessToken(7L, SUSPENDED_EMAIL, List.of("MODERATOR"));

        baselineGet(BASELINE + "/jobs-moderate", token)
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("H: DELETED account with a valid JWT is rejected (fail closed)")
    void deletedAccountWithValidJwtIsRejected() throws Exception {
        givenDbUser(dbUser(8L, DELETED_EMAIL, "DELETED", role("SUPER_ADMIN",
                "content:manage", "content:publish", "downloads:manage",
                "messages:manage", "users:manage", "jobs:manage", "jobs:moderate",
                "settings:manage")));
        String token = jwtTokenProvider.generateAccessToken(8L, DELETED_EMAIL, List.of("SUPER_ADMIN"));

        baselineGet(BASELINE + "/users-manage", token)
                .andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------
    // J. /api/v1/auth/me
    // ------------------------------------------------------------------

    @Test
    @DisplayName("J: /auth/me is rejected for anonymous callers with the standard envelope")
    void authMeAnonymousIs401() throws Exception {
        mockMvc.perform(get(ApiPaths.AUTH_ME))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errors[0].code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("J: /auth/me returns the authenticated identity from the validated token and exposes no sensitive fields")
    void authMeAuthenticatedReturnsSafeIdentity() throws Exception {
        givenDbUser(dbUser(4L, ADMIN_EMAIL, "ACTIVE",
                role("ADMIN", "content:manage", "content:publish", "downloads:manage",
                        "messages:manage", "jobs:manage", "settings:manage")));

        var userInfo = AuthResponse.UserInfo.builder()
                .id(4L)
                .email(ADMIN_EMAIL)
                .fullName("Test User")
                .userType("STAFF")
                .status("ACTIVE")
                .roles(Set.of("ROLE_ADMIN"))
                .permissions(Set.of("content:manage", "settings:manage"))
                .build();
        when(authService.getCurrentUser(4L)).thenReturn(userInfo);

        String token = jwtTokenProvider.generateAccessToken(4L, ADMIN_EMAIL, List.of("ADMIN"));
        mockMvc.perform(get(ApiPaths.AUTH_ME).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value(ADMIN_EMAIL))
                // Security invariants: never expose secrets or internal fields.
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andExpect(jsonPath("$.data.refreshToken").doesNotExist());
    }

    // ------------------------------------------------------------------
    // K. Public endpoints must remain public; logout stays compatible
    // ------------------------------------------------------------------

    @Test
    @DisplayName("K: /health remains public")
    void healthRemainsPublic() throws Exception {
        mockMvc.perform(get(ApiPaths.HEALTH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("K: POST /auth/login remains public (I-2 lockout untouched; generic failure shape)")
    void loginRemainsPublic() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenReturn(AuthResponse.builder()
                .accessToken("access").refreshToken("refresh").tokenType("Bearer").expiresIn(900L)
                .build());

        LoginRequest request = new LoginRequest();
        request.setEmail("someone@example.com");
        request.setPassword("NotTheRealPassword1!");

        mockMvc.perform(post(ApiPaths.AUTH_LOGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("K: POST /auth/refresh remains public")
    void refreshRemainsPublic() throws Exception {
        when(authService.refreshToken(any(RefreshTokenRequest.class))).thenReturn(AuthResponse.builder()
                .accessToken("access").refreshToken("refresh").tokenType("Bearer").expiresIn(900L)
                .build());

        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("some-opaque-refresh-token");

        mockMvc.perform(post(ApiPaths.AUTH_REFRESH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("K: logout remains authentication-gated (anonymous → 401)")
    void logoutAnonymousIs401() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("some-opaque-refresh-token");

        mockMvc.perform(post(ApiPaths.AUTH_LOGOUT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("K: logout works for an authenticated user")
    void logoutAuthenticatedSucceeds() throws Exception {
        givenDbUser(dbUser(4L, ADMIN_EMAIL, "ACTIVE", role("ADMIN", "settings:manage")));
        doNothingOnLogout();

        String token = jwtTokenProvider.generateAccessToken(4L, ADMIN_EMAIL, List.of("ADMIN"));

        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("some-opaque-refresh-token");

        mockMvc.perform(post(ApiPaths.AUTH_LOGOUT)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    private void doNothingOnLogout() {
        org.mockito.Mockito.doNothing().when(authService).logout(org.mockito.ArgumentMatchers.anyString());
    }

    // ------------------------------------------------------------------
    // Map-based body sanity check for the 401 envelope (exact contract)
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Authentication failures expose no token or account details")
    void rejectionEnvelopeLeaksNothing() throws Exception {
        String body = mockMvc.perform(get(BASELINE + "/users-manage")
                        .header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        org.assertj.core.api.Assertions.assertThat(body)
                .doesNotContain("not-a-real-token")
                .doesNotContain("Bearer")
                .contains("\"success\":false");
    }
}
