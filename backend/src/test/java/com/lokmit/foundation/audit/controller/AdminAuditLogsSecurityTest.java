package com.lokmit.foundation.audit.controller;

import com.lokmit.foundation.audit.entity.AuditLog;
import com.lokmit.foundation.audit.repository.AuditLogRepository;
import com.lokmit.foundation.audit.service.AuditLogService;
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
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A7.5 security and behavior matrix for the audit-log endpoints, exercised
 * through the REAL Spring Security filter chain (real JwtAuthenticationFilter,
 * real JwtTokenProvider, real CustomUserDetailsService with mocked
 * repositories — identical to the A1–A7.4 matrix setups).
 *
 * <p>Coverage: anonymous 401; ADMIN with {@code notifications:manage} still
 * 403 (audit reads are deliberately NOT covered by that permission);
 * SUPER_ADMIN-only via {@code users:manage}; spoofed claims 403; filtered
 * pagination; DTO safety; and the append-only contract — no write endpoint
 * exists at all.</p>
 */
@WebMvcTest(controllers = AuditLogController.class)
@Import({SecurityConfig.class, CorsConfig.class, JwtKeyConfig.class,
        JwtTokenProvider.class, CustomUserDetailsService.class, SecurityUtils.class,
        AuditLogService.class})
@AutoConfigureMockMvc
class AdminAuditLogsSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuditLogRepository auditLogRepository;

    @MockitoBean
    private UserRepository userRepository;

    private static final String AUDIT_LOGS = ApiPaths.ADMIN_AUDIT_LOGS;

    private static final String SUPER_ADMIN_EMAIL = "superadmin@lokmitfoundation.org";
    private static final String ADMIN_EMAIL = "admin@lokmitfoundation.org";
    private static final String MODERATOR_EMAIL = "moderator@lokmitfoundation.org";

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void resetStubs() {
        reset(auditLogRepository, userRepository);
    }

    // ------------------------------------------------------------------
    // helpers (mirrors the A7.1–A7.4 matrix setups)
    // ------------------------------------------------------------------

    private User dbUser(long id, String email, Role... roles) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setPasswordHash("$2a$10$storedhashnotusedbyjwtfilter012345678901234567890123");
        user.setFullName("Test User");
        user.setUserType("STAFF");
        user.setStatus("ACTIVE");
        user.setEmailVerified(true);
        user.setCreatedAt(OffsetDateTime.now());
        user.setUpdatedAt(OffsetDateTime.now());
        user.setRoles(Set.of(roles));
        return user;
    }

    private Role role(String code, String... permissionCodes) {
        Role role = new Role();
        role.setCode(code);
        role.setPermissions(Set.of(permissionCodes).stream().map(c -> {
            Permission p = new Permission();
            p.setCode(c);
            return p;
        }).collect(Collectors.toSet()));
        return role;
    }

    private void givenDbUser(User user) {
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
    }

    private AuditLog dbAuditLog(long id) {
        AuditLog log = new AuditLog();
        log.setId(id);
        log.setActorUserId(2L);
        log.setAction("START_REVIEW");
        log.setEntityType("JOB_APPLICATION");
        log.setEntityId(30L);
        log.setDetails("{\"previousStatus\":\"SUBMITTED\",\"newStatus\":\"UNDER_REVIEW\"}");
        log.setCreatedAt(OffsetDateTime.now());
        return log;
    }

    // ------------------------------------------------------------------
    // A. Anonymous → 401
    // ------------------------------------------------------------------

    @Test
    @DisplayName("anonymous access is rejected with 401 on audit-log endpoints")
    void anonymousIs401Everywhere() throws Exception {
        mockMvc.perform(get(AUDIT_LOGS)).andExpect(status().isUnauthorized());
        mockMvc.perform(get(AUDIT_LOGS + "/1")).andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------
    // B. Permission matrix — audit is SUPER_ADMIN-only
    // ------------------------------------------------------------------

    @Test
    @DisplayName("MODERATOR gets 403 on audit-log endpoints")
    void moderatorGets403() throws Exception {
        Role moderator = role("MODERATOR", "jobs:moderate", "messages:manage");
        givenDbUser(dbUser(3L, MODERATOR_EMAIL, moderator));
        String token = "Bearer " + jwtTokenProvider.generateAccessToken(
                3L, MODERATOR_EMAIL, List.of("MODERATOR"));

        mockMvc.perform(get(AUDIT_LOGS).header("Authorization", token))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(AUDIT_LOGS + "/1").header("Authorization", token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ADMIN with notifications:manage still gets 403 — audit is NOT covered by it")
    void adminWithNotificationsManageStill403() throws Exception {
        // V16 grants notifications:manage to ADMIN, but audit reads are
        // deliberately guarded by the SUPER_ADMIN-only users:manage instead.
        Role admin = role("ADMIN", "dashboard:view", "content:manage", "settings:manage",
                "employment:manage", "notifications:manage");
        givenDbUser(dbUser(2L, ADMIN_EMAIL, admin));
        String token = "Bearer " + jwtTokenProvider.generateAccessToken(
                2L, ADMIN_EMAIL, List.of("ADMIN"));

        mockMvc.perform(get(AUDIT_LOGS).header("Authorization", token))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(AUDIT_LOGS + "/1").header("Authorization", token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("spoofed JWT role claims do not elevate privileges on audit logs")
    void spoofedJwtRoleClaimsDoNotElevate() throws Exception {
        Role candidate = role("CANDIDATE");
        givenDbUser(dbUser(6L, "spoof@lokmitfoundation.org", candidate));
        String spoofed = "Bearer " + jwtTokenProvider.generateAccessToken(
                6L, "spoof@lokmitfoundation.org", List.of("SUPER_ADMIN", "ADMIN"));

        mockMvc.perform(get(AUDIT_LOGS).header("Authorization", spoofed))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------
    // C. SUPER_ADMIN success paths
    // ------------------------------------------------------------------

    @Test
    @DisplayName("SUPER_ADMIN lists audit logs with filters and reads one record")
    void superAdminAuditAccess() throws Exception {
        Role superAdmin = role("SUPER_ADMIN", "users:manage", "dashboard:view",
                "content:manage", "content:publish", "downloads:manage", "messages:manage",
                "jobs:manage", "jobs:moderate", "settings:manage", "employment:manage",
                "candidates:manage", "notifications:manage");
        givenDbUser(dbUser(1L, SUPER_ADMIN_EMAIL, superAdmin));
        String auth = "Bearer " + jwtTokenProvider.generateAccessToken(
                1L, SUPER_ADMIN_EMAIL, List.of("SUPER_ADMIN"));

        when(auditLogRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenAnswer(inv -> new PageImpl<>(
                        List.of(dbAuditLog(9L)),
                        inv.getArgument(1, Pageable.class), 1));
        mockMvc.perform(get(AUDIT_LOGS).header("Authorization", auth)
                        .param("actorUserId", "2").param("entityType", "JOB_APPLICATION")
                        .param("entityId", "30").param("action", "START_REVIEW"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalItems").value(1))
                .andExpect(jsonPath("$.data.items[0].action").value("START_REVIEW"))
                .andExpect(jsonPath("$.data.items[0].actorUserId").value(2))
                .andExpect(jsonPath("$.data.items[0].details")
                        .value("{\"previousStatus\":\"SUBMITTED\",\"newStatus\":\"UNDER_REVIEW\"}"));

        when(auditLogRepository.findById(9L)).thenReturn(Optional.of(dbAuditLog(9L)));
        mockMvc.perform(get(AUDIT_LOGS + "/9").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.entityType").value("JOB_APPLICATION"));

        // unknown id → 404
        when(auditLogRepository.findById(77L)).thenReturn(Optional.empty());
        mockMvc.perform(get(AUDIT_LOGS + "/77").header("Authorization", auth))
                .andExpect(status().isNotFound());

        // pagination cap → 400
        mockMvc.perform(get(AUDIT_LOGS).header("Authorization", auth)
                        .param("size", "200"))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------
    // D. Append-only contract — NO write endpoint exists
    // ------------------------------------------------------------------

    @Test
    @DisplayName("audit logs are append-only: no POST/PATCH/DELETE endpoint exists")
    void noWriteEndpoints() throws Exception {
        Role superAdmin = role("SUPER_ADMIN", "users:manage");
        givenDbUser(dbUser(1L, SUPER_ADMIN_EMAIL, superAdmin));
        String auth = "Bearer " + jwtTokenProvider.generateAccessToken(
                1L, SUPER_ADMIN_EMAIL, List.of("SUPER_ADMIN"));

        mockMvc.perform(post(AUDIT_LOGS).header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"FORGED\"}"))
                .andExpect(status().isMethodNotAllowed());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete(AUDIT_LOGS + "/9").header("Authorization", auth))
                .andExpect(status().isMethodNotAllowed());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .patch(AUDIT_LOGS + "/9").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"FORGED\"}"))
                .andExpect(status().isMethodNotAllowed());
    }

    // ------------------------------------------------------------------
    // E. DTO safety
    // ------------------------------------------------------------------

    @Test
    @DisplayName("audit responses expose only safe fields — no user identity or security material")
    void dtoSafety() throws Exception {
        Role superAdmin = role("SUPER_ADMIN", "users:manage");
        givenDbUser(dbUser(1L, SUPER_ADMIN_EMAIL, superAdmin));
        String auth = "Bearer " + jwtTokenProvider.generateAccessToken(
                1L, SUPER_ADMIN_EMAIL, List.of("SUPER_ADMIN"));

        AuditLog log = dbAuditLog(9L);
        when(auditLogRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenAnswer(inv -> new PageImpl<>(List.of(log),
                        inv.getArgument(1, Pageable.class), 1));

        String body = mockMvc.perform(get(AUDIT_LOGS).header("Authorization", auth))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(body)
                .doesNotContain("password")
                .doesNotContain("token")
                .doesNotContain("@lokmitfoundation.org")
                .doesNotContain("userType")
                .doesNotContain("\"roles\"");
    }
}
