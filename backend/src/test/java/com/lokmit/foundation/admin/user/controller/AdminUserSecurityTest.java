package com.lokmit.foundation.admin.user.controller;

import com.lokmit.foundation.admin.user.dto.AdminUserResponse;
import com.lokmit.foundation.admin.user.service.AdminUserService;
import com.lokmit.foundation.common.constants.ApiPaths;
import com.lokmit.foundation.security.config.CorsConfig;
import com.lokmit.foundation.security.config.JwtKeyConfig;
import com.lokmit.foundation.security.config.SecurityConfig;
import com.lokmit.foundation.security.entity.Permission;
import com.lokmit.foundation.security.entity.Role;
import com.lokmit.foundation.security.entity.User;
import com.lokmit.foundation.security.repository.UserRepository;
import com.lokmit.foundation.security.service.CustomUserDetailsService;
import com.lokmit.foundation.security.service.JwtTokenProvider;
import com.lokmit.foundation.security.util.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Admin User Management security and behavior matrix (A3), exercised through
 * the REAL Spring Security filter chain (real JwtAuthenticationFilter, real
 * JwtTokenProvider, real CustomUserDetailsService with a mocked repository —
 * identical to the A1/A2 matrix setups).
 *
 * <p>Coverage:</p>
 * <ul>
 *   <li>anonymous → 401 on every user-management endpoint</li>
 *   <li>authenticated without users:manage (MODERATOR, ADMIN, CANDIDATE) → 403</li>
 *   <li>SUPER_ADMIN (users:manage via V2) → 200</li>
 *   <li>JWT roles-claim spoofing does not grant user-management access</li>
 *   <li>status/role filter validation → 400</li>
 *   <li>protection rules over the wire: self-demotion 400, unknown user 404</li>
 *   <li>no security-sensitive fields in any response body</li>
 * </ul>
 */
@WebMvcTest(controllers = AdminUserController.class)
@Import({SecurityConfig.class, CorsConfig.class, JwtKeyConfig.class,
        JwtTokenProvider.class, CustomUserDetailsService.class, SecurityUtils.class,
        AdminUserService.class})
@AutoConfigureMockMvc
class AdminUserSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    // The REAL AdminUserService runs (protection rules included); only the
    // persistence boundary is mocked, so tests exercise controller → service.
    @MockitoBean
    private com.lokmit.foundation.security.repository.RoleRepository roleRepository;

    @MockitoBean
    private com.lokmit.foundation.security.repository.RefreshTokenRepository refreshTokenRepository;

    @MockitoBean
    private UserRepository userRepository;

    private static final String BASE = ApiPaths.ADMIN_USERS;
    private static final String MODERATOR_EMAIL = "moderator@lokmitfoundation.org";
    private static final String ADMIN_EMAIL = "admin@lokmitfoundation.org";
    private static final String SUPER_ADMIN_EMAIL = "superadmin@lokmitfoundation.org";
    private static final String CANDIDATE_EMAIL = "candidate@lokmitfoundation.org";

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void stubSaveEcho() {
        // The REAL AdminUserService runs; make the mocked repository echo
        // saves back the way JPA would.
        org.mockito.Mockito.when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
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

    private AdminUserResponse sampleUser(long id, String email, String status) {
        return AdminUserResponse.builder()
                .id(id)
                .email(email)
                .fullName("Test User")
                .userType("STAFF")
                .status(status)
                .emailVerified(true)
                .roles(Set.of("MODERATOR"))
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
    }

    // ------------------------------------------------------------------
    // A. Anonymous → 401
    // ------------------------------------------------------------------

    @Test
    @DisplayName("anonymous list is rejected with 401 envelope")
    void anonymousListIs401() throws Exception {
        mockMvc.perform(get(BASE))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errors[0].code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("anonymous detail/status/roles calls are rejected with 401")
    void anonymousMutationsAre401() throws Exception {
        mockMvc.perform(get(BASE + "/5")).andExpect(status().isUnauthorized());
        mockMvc.perform(patch(BASE + "/5/status")
                        .contentType("application/json").content("{\"status\":\"SUSPENDED\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put(BASE + "/5/roles")
                        .contentType("application/json").content("{\"roles\":[\"ADMIN\"]}"))
                .andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------
    // B. Authenticated without users:manage → 403
    // ------------------------------------------------------------------

    @Test
    @DisplayName("MODERATOR (no users:manage) gets 403 on all user-management endpoints")
    void moderatorIs403() throws Exception {
        givenDbUser(dbUser(3L, MODERATOR_EMAIL, "ACTIVE",
                role("MODERATOR", "jobs:moderate", "messages:manage")));
        String token = jwtTokenProvider.generateAccessToken(3L, MODERATOR_EMAIL, List.of("MODERATOR"));

        mockMvc.perform(get(BASE).header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(BASE + "/5").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch(BASE + "/5/status").header("Authorization", "Bearer " + token)
                        .contentType("application/json").content("{\"status\":\"SUSPENDED\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(put(BASE + "/5/roles").header("Authorization", "Bearer " + token)
                        .contentType("application/json").content("{\"roles\":[\"ADMIN\"]}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ADMIN (no users:manage in the V2 seed) gets 403")
    void adminIs403() throws Exception {
        givenDbUser(dbUser(4L, ADMIN_EMAIL, "ACTIVE",
                role("ADMIN", "dashboard:view", "content:manage", "content:publish",
                        "downloads:manage", "messages:manage", "jobs:manage", "settings:manage")));
        String token = jwtTokenProvider.generateAccessToken(4L, ADMIN_EMAIL, List.of("ADMIN"));

        mockMvc.perform(get(BASE).header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("CANDIDATE (no permissions) gets 403")
    void candidateIs403() throws Exception {
        givenDbUser(dbUser(2L, CANDIDATE_EMAIL, "ACTIVE", role("CANDIDATE")));
        String token = jwtTokenProvider.generateAccessToken(2L, CANDIDATE_EMAIL, List.of("CANDIDATE"));

        mockMvc.perform(get(BASE).header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------
    // C. SUPER_ADMIN → 200
    // ------------------------------------------------------------------

    @Test
    @DisplayName("SUPER_ADMIN lists users with pagination envelope")
    void superAdminListsUsers() throws Exception {
        givenDbUser(dbUser(1L, SUPER_ADMIN_EMAIL, "ACTIVE", role("SUPER_ADMIN",
                "users:manage", "dashboard:view", "content:manage", "content:publish",
                "downloads:manage", "messages:manage", "jobs:manage", "jobs:moderate",
                "settings:manage")));
        String token = jwtTokenProvider.generateAccessToken(1L, SUPER_ADMIN_EMAIL, List.of("SUPER_ADMIN"));

        when(userRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(
                        dbUser(2L, "u2@example.com", "ACTIVE", role("MODERATOR", "messages:manage"))),
                        org.springframework.data.domain.PageRequest.of(0, 20), 1));

        mockMvc.perform(get(BASE).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].email").value("u2@example.com"))
                .andExpect(jsonPath("$.data.totalItems").value(1));
    }

    @Test
    @DisplayName("SUPER_ADMIN gets a user detail view")
    void superAdminGetsUserDetail() throws Exception {
        givenDbUser(dbUser(1L, SUPER_ADMIN_EMAIL, "ACTIVE", role("SUPER_ADMIN", "users:manage")));
        String token = jwtTokenProvider.generateAccessToken(1L, SUPER_ADMIN_EMAIL, List.of("SUPER_ADMIN"));

        when(userRepository.findById(5L)).thenReturn(Optional.of(
                dbUser(5L, "target@example.com", "ACTIVE", role("MODERATOR", "messages:manage"))));

        mockMvc.perform(get(BASE + "/5").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("target@example.com"))
                .andExpect(jsonPath("$.data.roles[0]").value("MODERATOR"));
    }

    // ------------------------------------------------------------------
    // D. JWT claim spoofing gains nothing
    // ------------------------------------------------------------------

    @Test
    @DisplayName("spoofed SUPER_ADMIN role claim does not grant users:manage")
    void spoofedClaimIs403() throws Exception {
        givenDbUser(dbUser(9L, CANDIDATE_EMAIL, "ACTIVE", role("CANDIDATE")));
        String token = jwtTokenProvider.generateAccessToken(9L, CANDIDATE_EMAIL,
                List.of("SUPER_ADMIN", "ADMIN"));

        mockMvc.perform(get(BASE).header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------
    // E. Filter validation → 400
    // ------------------------------------------------------------------

    @Test
    @DisplayName("invalid status and role filter values are rejected with 400")
    void invalidFiltersAre400() throws Exception {
        givenDbUser(dbUser(1L, SUPER_ADMIN_EMAIL, "ACTIVE", role("SUPER_ADMIN", "users:manage")));
        String token = jwtTokenProvider.generateAccessToken(1L, SUPER_ADMIN_EMAIL, List.of("SUPER_ADMIN"));

        mockMvc.perform(get(BASE).header("Authorization", "Bearer " + token)
                        .param("status", "BANNED"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        mockMvc.perform(get(BASE).header("Authorization", "Bearer " + token)
                        .param("role", "HACKER"))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------
    // F. Behavior: status change, protections, 404, page-size cap
    // ------------------------------------------------------------------

    @Test
    @DisplayName("status change reaches the service with the authenticated actor id")
    void statusChangeCarriesActor() throws Exception {
        givenDbUser(dbUser(1L, SUPER_ADMIN_EMAIL, "ACTIVE", role("SUPER_ADMIN", "users:manage")));
        String token = jwtTokenProvider.generateAccessToken(1L, SUPER_ADMIN_EMAIL, List.of("SUPER_ADMIN"));
        when(userRepository.findById(5L)).thenReturn(Optional.of(
                dbUser(5L, "target@example.com", "ACTIVE", role("MODERATOR", "messages:manage"))));

        mockMvc.perform(patch(BASE + "/5/status").header("Authorization", "Bearer " + token)
                        .contentType("application/json").content("{\"status\":\"SUSPENDED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUSPENDED"));

        // The service loaded the real target user and revoked refresh tokens.
        verify(refreshTokenRepository).revokeAllByUserId(eq(5L), any(OffsetDateTime.class));
    }

    @Test
    @DisplayName("an administrator cannot deactivate their own account (400)")
    void selfDemotionIs400() throws Exception {
        givenDbUser(dbUser(1L, SUPER_ADMIN_EMAIL, "ACTIVE", role("SUPER_ADMIN", "users:manage")));
        String token = jwtTokenProvider.generateAccessToken(1L, SUPER_ADMIN_EMAIL, List.of("SUPER_ADMIN"));

        mockMvc.perform(patch(BASE + "/1/status").header("Authorization", "Bearer " + token)
                        .contentType("application/json").content("{\"status\":\"SUSPENDED\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("unknown user id returns the standard 404 envelope")
    void unknownUserIs404() throws Exception {
        givenDbUser(dbUser(1L, SUPER_ADMIN_EMAIL, "ACTIVE", role("SUPER_ADMIN", "users:manage")));
        String token = jwtTokenProvider.generateAccessToken(1L, SUPER_ADMIN_EMAIL, List.of("SUPER_ADMIN"));
        when(userRepository.findById(404L)).thenReturn(Optional.empty());

        mockMvc.perform(get(BASE + "/404").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errors[0].code").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("oversized page size is rejected by shared PageParams validation (400)")
    void oversizedPageIs400() throws Exception {
        givenDbUser(dbUser(1L, SUPER_ADMIN_EMAIL, "ACTIVE", role("SUPER_ADMIN", "users:manage")));
        String token = jwtTokenProvider.generateAccessToken(1L, SUPER_ADMIN_EMAIL, List.of("SUPER_ADMIN"));

        mockMvc.perform(get(BASE).header("Authorization", "Bearer " + token)
                        .param("size", "100000"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("responses never contain security-sensitive fields")
    void noSensitiveFieldsLeak() throws Exception {
        givenDbUser(dbUser(1L, SUPER_ADMIN_EMAIL, "ACTIVE", role("SUPER_ADMIN", "users:manage")));
        String token = jwtTokenProvider.generateAccessToken(1L, SUPER_ADMIN_EMAIL, List.of("SUPER_ADMIN"));
        when(userRepository.findById(5L)).thenReturn(Optional.of(
                dbUser(5L, "target@example.com", "ACTIVE", role("MODERATOR", "messages:manage"))));

        String body = mockMvc.perform(get(BASE + "/5").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(body)
                .doesNotContain("passwordHash", "password_hash", "failed_login_attempts",
                        "failedLoginAttempts", "locked_until", "lockedUntil",
                        "failedLoginWindow", "refreshToken", "tokenHash");
    }
}
