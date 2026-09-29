package com.lokmit.foundation.notification.controller;

import com.lokmit.foundation.common.constants.ApiPaths;
import com.lokmit.foundation.notification.entity.Notification;
import com.lokmit.foundation.notification.repository.NotificationRepository;
import com.lokmit.foundation.notification.service.NotificationService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A7.5 security and behavior matrix for the notification endpoints,
 * exercised through the REAL Spring Security filter chain (real
 * JwtAuthenticationFilter, real JwtTokenProvider, real
 * CustomUserDetailsService with mocked repositories — identical to the
 * A1–A7.4 matrix setups).
 *
 * <p>Coverage: anonymous 401; roles without {@code notifications:manage}
 * get 403; spoofed SUPER_ADMIN claims 403; ADMIN/SUPER_ADMIN (the two roles
 * V16 grants) succeed; every operation stays recipient-scoped to the
 * authenticated user resolved server-side — {@code notifications:manage}
 * never broadens scoping; DTO safety; pagination cap.</p>
 */
@WebMvcTest(controllers = NotificationController.class)
@Import({SecurityConfig.class, CorsConfig.class, JwtKeyConfig.class,
        JwtTokenProvider.class, CustomUserDetailsService.class, SecurityUtils.class,
        NotificationService.class})
@AutoConfigureMockMvc
class AdminNotificationsSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationRepository notificationRepository;

    @MockitoBean
    private UserRepository userRepository;

    private static final String NOTIFICATIONS = ApiPaths.ADMIN_NOTIFICATIONS;

    private static final String SUPER_ADMIN_EMAIL = "superadmin@lokmitfoundation.org";
    private static final String ADMIN_EMAIL = "admin@lokmitfoundation.org";
    private static final String MODERATOR_EMAIL = "moderator@lokmitfoundation.org";
    private static final String CANDIDATE_EMAIL = "candidate@lokmitfoundation.org";

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void resetStubs() {
        reset(notificationRepository, userRepository);
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

    private String adminAuth() {
        Role admin = role("ADMIN", "dashboard:view", "content:manage", "content:publish",
                "downloads:manage", "messages:manage", "jobs:manage", "settings:manage",
                "employment:manage", "candidates:manage", "notifications:manage");
        givenDbUser(dbUser(2L, ADMIN_EMAIL, admin));
        return "Bearer " + jwtTokenProvider.generateAccessToken(
                2L, ADMIN_EMAIL, List.of("ADMIN"));
    }

    private Notification dbNotification(long id, long recipient, boolean read) {
        Notification n = new Notification();
        n.setId(id);
        n.setRecipientUserId(recipient);
        n.setType(Notification.TYPE_APPLICATION_STATUS_CHANGED);
        n.setTitle("Application status updated: HIRED");
        n.setBody("Your application moved from UNDER_REVIEW to HIRED.");
        n.setEntityType("JOB_APPLICATION");
        n.setEntityId(30L);
        n.setReadAt(read ? OffsetDateTime.now() : null);
        n.setCreatedAt(OffsetDateTime.now());
        return n;
    }

    // ------------------------------------------------------------------
    // A. Anonymous → 401 on every endpoint
    // ------------------------------------------------------------------

    @Test
    @DisplayName("anonymous access is rejected with 401 on all notification endpoints")
    void anonymousIs401Everywhere() throws Exception {
        mockMvc.perform(get(NOTIFICATIONS)).andExpect(status().isUnauthorized());
        mockMvc.perform(get(NOTIFICATIONS + "/unread-count"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get(NOTIFICATIONS + "/1")).andExpect(status().isUnauthorized());
        mockMvc.perform(patch(NOTIFICATIONS + "/1/read"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(patch(NOTIFICATIONS + "/1/unread"))
                .andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------
    // B. Permission matrix
    // ------------------------------------------------------------------

    @Test
    @DisplayName("MODERATOR and CANDIDATE-role users (no notifications:manage) get 403")
    void unauthorizedRolesGet403() throws Exception {
        Role moderator = role("MODERATOR", "jobs:moderate", "messages:manage");
        givenDbUser(dbUser(3L, MODERATOR_EMAIL, moderator));
        String moderatorToken = "Bearer " + jwtTokenProvider.generateAccessToken(
                3L, MODERATOR_EMAIL, List.of("MODERATOR"));
        mockMvc.perform(get(NOTIFICATIONS).header("Authorization", moderatorToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch(NOTIFICATIONS + "/1/read")
                        .header("Authorization", moderatorToken))
                .andExpect(status().isForbidden());

        Role candidate = role("CANDIDATE");
        givenDbUser(dbUser(5L, CANDIDATE_EMAIL, candidate));
        String candidateToken = "Bearer " + jwtTokenProvider.generateAccessToken(
                5L, CANDIDATE_EMAIL, List.of("CANDIDATE"));
        mockMvc.perform(get(NOTIFICATIONS + "/unread-count")
                        .header("Authorization", candidateToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("spoofed JWT role claims do not elevate privileges on notifications")
    void spoofedJwtRoleClaimsDoNotElevate() throws Exception {
        Role candidate = role("CANDIDATE");
        givenDbUser(dbUser(6L, "spoof@lokmitfoundation.org", candidate));
        String spoofed = "Bearer " + jwtTokenProvider.generateAccessToken(
                6L, "spoof@lokmitfoundation.org", List.of("SUPER_ADMIN", "ADMIN"));
        mockMvc.perform(get(NOTIFICATIONS).header("Authorization", spoofed))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch(NOTIFICATIONS + "/1/read")
                        .header("Authorization", spoofed))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------
    // C. ADMIN / SUPER_ADMIN success + recipient scoping
    // ------------------------------------------------------------------

    @Test
    @DisplayName("ADMIN lists, reads and marks own notifications; scoping is recipient-bound")
    void adminNotificationManagement() throws Exception {
        String auth = adminAuth();

        // list (recipient-scoped by the service's Specification)
        when(notificationRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenAnswer(inv -> new PageImpl<>(
                        List.of(dbNotification(1L, 2L, false)),
                        inv.getArgument(1, Pageable.class), 1));
        mockMvc.perform(get(NOTIFICATIONS).header("Authorization", auth)
                        .param("type", "APPLICATION_STATUS_CHANGED").param("unreadOnly", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalItems").value(1))
                .andExpect(jsonPath("$.data.items[0].title")
                        .value("Application status updated: HIRED"))
                .andExpect(jsonPath("$.data.items[0].readAt").doesNotExist());

        // unread count shape
        when(notificationRepository.countByRecipientUserIdAndReadAtIsNull(2L))
                .thenReturn(4L);
        mockMvc.perform(get(NOTIFICATIONS + "/unread-count")
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount").value(4));

        // get owned
        when(notificationRepository.findByIdAndRecipientUserId(1L, 2L))
                .thenReturn(Optional.of(dbNotification(1L, 2L, false)));
        mockMvc.perform(get(NOTIFICATIONS + "/1").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.entityType").value("JOB_APPLICATION"));

        // foreign (id, recipient) pair → plain 404
        when(notificationRepository.findByIdAndRecipientUserId(99L, 2L))
                .thenReturn(Optional.empty());
        mockMvc.perform(get(NOTIFICATIONS + "/99").header("Authorization", auth))
                .andExpect(status().isNotFound());

        // mark read → timestamp stamped; repeat keeps it (idempotent)
        Notification owned = dbNotification(1L, 2L, false);
        when(notificationRepository.findByIdAndRecipientUserId(1L, 2L))
                .thenReturn(Optional.of(owned));
        // JPA save returns the managed entity; mirror that contract.
        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        mockMvc.perform(patch(NOTIFICATIONS + "/1/read").header("Authorization", auth))
                .andExpect(status().isOk());
        assertThat(owned.getReadAt()).isNotNull();

        // mark unread → cleared
        mockMvc.perform(patch(NOTIFICATIONS + "/1/unread").header("Authorization", auth))
                .andExpect(status().isOk());
        assertThat(owned.getReadAt()).isNull();

        // pagination cap → 400
        mockMvc.perform(get(NOTIFICATIONS).header("Authorization", auth)
                        .param("size", "200"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("SUPER_ADMIN (notifications:manage per V16) can use the notification APIs")
    void superAdminNotificationAccess() throws Exception {
        Role superAdmin = role("SUPER_ADMIN", "users:manage", "dashboard:view",
                "content:manage", "content:publish", "downloads:manage", "messages:manage",
                "jobs:manage", "jobs:moderate", "settings:manage", "employment:manage",
                "candidates:manage", "notifications:manage");
        givenDbUser(dbUser(1L, SUPER_ADMIN_EMAIL, superAdmin));
        String auth = "Bearer " + jwtTokenProvider.generateAccessToken(
                1L, SUPER_ADMIN_EMAIL, List.of("SUPER_ADMIN"));

        when(notificationRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenAnswer(inv -> new PageImpl<>(List.of(), 
                        inv.getArgument(1, Pageable.class), 0));
        mockMvc.perform(get(NOTIFICATIONS).header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.totalItems").value(0));
    }

    // ------------------------------------------------------------------
    // D. DTO safety
    // ------------------------------------------------------------------

    @Test
    @DisplayName("notification responses never expose security fields or recipient identity")
    void dtoSafety() throws Exception {
        String auth = adminAuth();
        when(notificationRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenAnswer(inv -> new PageImpl<>(
                        List.of(dbNotification(1L, 2L, false)),
                        inv.getArgument(1, Pageable.class), 1));

        String body = mockMvc.perform(get(NOTIFICATIONS).header("Authorization", auth))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(body)
                .doesNotContain("password")
                .doesNotContain("token")
                .doesNotContain("recipientUserId")
                .doesNotContain("@lokmitfoundation.org")
                .doesNotContain("\"roles\"");
    }
}
