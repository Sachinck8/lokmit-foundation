package com.lokmit.foundation.dashboard.controller;

import com.lokmit.foundation.common.constants.ApiPaths;
import com.lokmit.foundation.dashboard.dto.DashboardApplicationResponse;
import com.lokmit.foundation.dashboard.dto.DashboardEnquiryResponse;
import com.lokmit.foundation.dashboard.dto.DashboardSummaryResponse;
import com.lokmit.foundation.dashboard.dto.DashboardUserResponse;
import com.lokmit.foundation.dashboard.service.DashboardService;
import com.lokmit.foundation.dashboard.repository.DashboardRepository;
import com.lokmit.foundation.dashboard.service.DashboardService;
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
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static com.lokmit.foundation.dashboard.service.DashboardService.DEFAULT_LIMIT;
import static com.lokmit.foundation.dashboard.service.DashboardService.MAX_LIMIT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Admin Dashboard security and behavior matrix (A2), exercised through the
 * REAL Spring Security filter chain (real JwtAuthenticationFilter, real
 * JwtTokenProvider, real CustomUserDetailsService with a mocked repository —
 * identical to the A1 matrix setup).
 *
 * <p>Coverage:</p>
 * <ul>
 *   <li>anonymous → 401 on every dashboard endpoint</li>
 *   <li>authenticated without dashboard:view (MODERATOR, CANDIDATE) → 403</li>
 *   <li>ADMIN and SUPER_ADMIN (dashboard:view via V11) → 200</li>
 *   <li>JWT roles-claim spoofing does not grant dashboard access</li>
 *   <li>limit clamping over the wire (11 → 10, 100000 → 10, absent → 5)</li>
 *   <li>no security-sensitive fields in the response body</li>
 * </ul>
 */
@WebMvcTest(controllers = AdminDashboardController.class)
@Import({SecurityConfig.class, CorsConfig.class, JwtKeyConfig.class,
        JwtTokenProvider.class, CustomUserDetailsService.class, SecurityUtils.class,
        DashboardService.class})
@AutoConfigureMockMvc
class AdminDashboardSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    // The REAL DashboardService runs (clamping included); only the persistence
    // boundary is mocked, so the tests exercise controller → service → SQL contract.
    @MockitoBean
    private DashboardRepository dashboardRepository;

    @MockitoBean
    private UserRepository userRepository;

    private static final String BASE = ApiPaths.ADMIN_DASHBOARD;
    private static final String MODERATOR_EMAIL = "moderator@lokmitfoundation.org";
    private static final String ADMIN_EMAIL = "admin@lokmitfoundation.org";
    private static final String SUPER_ADMIN_EMAIL = "superadmin@lokmitfoundation.org";
    private static final String CANDIDATE_EMAIL = "candidate@lokmitfoundation.org";

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void resetStubs() {
        org.mockito.Mockito.reset(userRepository, dashboardRepository);
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

    private ResultActions getWithToken(String path, String token) throws Exception {
        return mockMvc.perform(get(path).header("Authorization", "Bearer " + token));
    }

    // ------------------------------------------------------------------
    // A. Anonymous → 401
    // ------------------------------------------------------------------

    @Test
    @DisplayName("anonymous summary is rejected with 401 envelope")
    void anonymousSummaryIs401() throws Exception {
        mockMvc.perform(get(BASE + "/summary"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errors[0].code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("anonymous recent lists are rejected with 401")
    void anonymousRecentListsAre401() throws Exception {
        for (String path : List.of("/recent-enquiries", "/recent-users", "/recent-applications")) {
            mockMvc.perform(get(BASE + path)).andExpect(status().isUnauthorized());
        }
    }

    // ------------------------------------------------------------------
    // B. Authenticated without dashboard:view → 403
    // ------------------------------------------------------------------

    @Test
    @DisplayName("MODERATOR (no dashboard:view) gets 403 on all dashboard endpoints")
    void moderatorIs403() throws Exception {
        givenDbUser(dbUser(3L, MODERATOR_EMAIL, "ACTIVE",
                role("MODERATOR", "jobs:moderate", "messages:manage")));
        String token = jwtTokenProvider.generateAccessToken(3L, MODERATOR_EMAIL, List.of("MODERATOR"));

        getWithToken(BASE + "/summary", token).andExpect(status().isForbidden());
        getWithToken(BASE + "/recent-enquiries", token).andExpect(status().isForbidden());
        getWithToken(BASE + "/recent-users", token).andExpect(status().isForbidden());
        getWithToken(BASE + "/recent-applications", token).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("CANDIDATE (no permissions) gets 403 on the dashboard")
    void candidateIs403() throws Exception {
        givenDbUser(dbUser(2L, CANDIDATE_EMAIL, "ACTIVE", role("CANDIDATE")));
        String token = jwtTokenProvider.generateAccessToken(2L, CANDIDATE_EMAIL, List.of("CANDIDATE"));

        getWithToken(BASE + "/summary", token).andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------
    // Claim spoofing still fails (A1 invariant)
    // ------------------------------------------------------------------

    @Test
    @DisplayName("a spoofed SUPER_ADMIN roles claim does not grant dashboard access")
    void spoofedRoleClaimDoesNotGrantAccess() throws Exception {
        givenDbUser(dbUser(2L, CANDIDATE_EMAIL, "ACTIVE", role("CANDIDATE")));
        String spoofed = jwtTokenProvider.generateAccessToken(
                2L, CANDIDATE_EMAIL, List.of("SUPER_ADMIN"));

        getWithToken(BASE + "/summary", spoofed).andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------
    // D/E. ADMIN + SUPER_ADMIN → 200
    // ------------------------------------------------------------------

    @Test
    @DisplayName("ADMIN with dashboard:view reads summary and recent lists")
    void adminCanReadEverything() throws Exception {
        givenDbUser(dbUser(4L, ADMIN_EMAIL, "ACTIVE",
                role("ADMIN", "content:manage", "content:publish", "downloads:manage",
                        "messages:manage", "jobs:manage", "settings:manage", "dashboard:view")));
        String token = jwtTokenProvider.generateAccessToken(4L, ADMIN_EMAIL, List.of("ADMIN"));

        when(dashboardRepository.summaryCounts()).thenReturn(DashboardSummaryResponse.builder().build());
        when(dashboardRepository.recentEnquiries(anyInt())).thenReturn(List.of(
                DashboardEnquiryResponse.builder().id(42L).name("Ramesh Kumar")
                        .email("ramesh@example.com").subject("Enquiry").status("NEW")
                        .createdAt(OffsetDateTime.now()).build()));
        when(dashboardRepository.recentUsers(anyInt())).thenReturn(List.of());
        when(dashboardRepository.recentApplications(anyInt())).thenReturn(List.of());

        getWithToken(BASE + "/summary", token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
        getWithToken(BASE + "/recent-enquiries", token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(42))
                .andExpect(jsonPath("$.data[0].name").value("Ramesh Kumar"));
        getWithToken(BASE + "/recent-users", token)
                .andExpect(status().isOk());
        getWithToken(BASE + "/recent-applications", token)
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("SUPER_ADMIN with dashboard:view reads the summary")
    void superAdminCanReadSummary() throws Exception {
        givenDbUser(dbUser(5L, SUPER_ADMIN_EMAIL, "ACTIVE",
                role("SUPER_ADMIN", "content:manage", "content:publish", "downloads:manage",
                        "messages:manage", "users:manage", "jobs:manage", "jobs:moderate",
                        "settings:manage", "dashboard:view")));
        String token = jwtTokenProvider.generateAccessToken(5L, SUPER_ADMIN_EMAIL, List.of("SUPER_ADMIN"));

        when(dashboardRepository.summaryCounts()).thenReturn(DashboardSummaryResponse.builder().build());

        getWithToken(BASE + "/summary", token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // ------------------------------------------------------------------
    // Limit handling over the wire (real service clamps before the repository)
    // ------------------------------------------------------------------

    @Test
    @DisplayName("limit defaults to 5 and is clamped to 10 before reaching the repository")
    void limitIsClampedOverTheWire() throws Exception {
        givenDbUser(dbUser(4L, ADMIN_EMAIL, "ACTIVE",
                role("ADMIN", "dashboard:view")));
        String token = jwtTokenProvider.generateAccessToken(4L, ADMIN_EMAIL, List.of("ADMIN"));
        when(dashboardRepository.recentUsers(anyInt())).thenReturn(List.of());

        // Absent parameter → real service clamps to the default of 5.
        mockMvc.perform(get(BASE + "/recent-users").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Abuse attempt (limit=100000) → clamped to the maximum of 10.
        mockMvc.perform(get(BASE + "/recent-users")
                        .header("Authorization", "Bearer " + token)
                        .param("limit", "100000"))
                .andExpect(status().isOk());

        ArgumentCaptor<Integer> limitCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(dashboardRepository, org.mockito.Mockito.times(2)).recentUsers(limitCaptor.capture());
        org.assertj.core.api.Assertions.assertThat(limitCaptor.getAllValues())
                .containsExactly(DEFAULT_LIMIT, MAX_LIMIT);
    }

    // ------------------------------------------------------------------
    // Response safety
    // ------------------------------------------------------------------

    @Test
    @DisplayName("recent-users response body contains no security-sensitive fields")
    void recentUsersBodyIsSafe() throws Exception {
        givenDbUser(dbUser(4L, ADMIN_EMAIL, "ACTIVE", role("ADMIN", "dashboard:view")));
        String token = jwtTokenProvider.generateAccessToken(4L, ADMIN_EMAIL, List.of("ADMIN"));

        when(dashboardRepository.recentUsers(anyInt())).thenReturn(List.of(
                DashboardUserResponse.builder().id(17L).email("priya.sharma@example.com")
                        .fullName("Priya Sharma").userType("CANDIDATE").status("ACTIVE")
                        .createdAt(OffsetDateTime.now()).lastLoginAt(OffsetDateTime.now())
                        .build()));

        String body = mockMvc.perform(get(BASE + "/recent-users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);

        org.assertj.core.api.Assertions.assertThat(body)
                .doesNotContain("passwordHash")
                .doesNotContain("password")
                .doesNotContain("refreshToken")
                .doesNotContain("token")
                .doesNotContain("roles")
                .doesNotContain("permissions");
    }
}
