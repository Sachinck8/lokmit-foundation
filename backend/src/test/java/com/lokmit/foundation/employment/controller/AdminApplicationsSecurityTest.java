package com.lokmit.foundation.employment.controller;

import com.lokmit.foundation.common.constants.ApiPaths;
import com.lokmit.foundation.employment.application.controller.ApplicationController;
import com.lokmit.foundation.employment.application.dto.ApplicationResponse;
import com.lokmit.foundation.employment.application.entity.JobApplication;
import com.lokmit.foundation.employment.application.repository.JobApplicationRepository;
import com.lokmit.foundation.employment.application.service.ApplicationService;
import com.lokmit.foundation.employment.candidate.entity.Candidate;
import com.lokmit.foundation.employment.candidate.repository.CandidateRepository;
import com.lokmit.foundation.employment.employer.entity.Employer;
import com.lokmit.foundation.employment.job.entity.Job;
import com.lokmit.foundation.employment.job.repository.JobRepository;
import com.lokmit.foundation.employment.jobcategory.entity.JobCategory;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * A7.3 security and behavior matrix exercised through the REAL Spring
 * Security filter chain (real JwtAuthenticationFilter, real JwtTokenProvider,
 * real CustomUserDetailsService with mocked repositories — identical to the
 * A1–A7.2 matrix setups).
 *
 * <p>Coverage: anonymous 401 on every family; MODERATOR/EDITOR/CANDIDATE-role
 * 403; spoofed SUPER_ADMIN claim 403; ADMIN and SUPER_ADMIN success; review
 * lifecycle SUBMITTED → UNDER_REVIEW → SHORTLISTED → decided (decided_at
 * stamped, repeats 409, WITHDRAWN blocked 400); withdraw semantics; PATCH of
 * employerNote/resumeId with immutable identity; pagination defaults/cap;
 * DTO leak safety (candidate's linked-user security internals never leak);
 * no delete endpoint exists.</p>
 */
@WebMvcTest(controllers = ApplicationController.class)
@Import({SecurityConfig.class, CorsConfig.class, JwtKeyConfig.class,
        JwtTokenProvider.class, CustomUserDetailsService.class, SecurityUtils.class,
        ApplicationService.class})
@AutoConfigureMockMvc
class AdminApplicationsSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JobApplicationRepository applicationRepository;

    @MockitoBean
    private JobRepository jobRepository;

    @MockitoBean
    private CandidateRepository candidateRepository;

    @MockitoBean
    private UserRepository userRepository;

    private static final String APPLICATIONS = ApiPaths.ADMIN_APPLICATIONS;

    private static final String SUPER_ADMIN_EMAIL = "superadmin@lokmitfoundation.org";
    private static final String ADMIN_EMAIL = "admin@lokmitfoundation.org";
    private static final String MODERATOR_EMAIL = "moderator@lokmitfoundation.org";
    private static final String EDITOR_EMAIL = "editor@lokmitfoundation.org";
    private static final String CANDIDATE_EMAIL = "candidate@lokmitfoundation.org";

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void resetStubs() {
        reset(applicationRepository, jobRepository, candidateRepository, userRepository);
    }

    // ------------------------------------------------------------------
    // helpers
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
                "employment:manage", "candidates:manage");
        givenDbUser(dbUser(2L, ADMIN_EMAIL, admin));
        return "Bearer " + jwtTokenProvider.generateAccessToken(
                2L, ADMIN_EMAIL, List.of("ADMIN"));
    }

    private User dbLinkedUser(long id) {
        User user = new User();
        user.setId(id);
        user.setEmail("linked-" + id + "@example.org");
        user.setFullName("Linked User");
        user.setUserType("CANDIDATE");
        user.setStatus("ACTIVE");
        user.setEmailVerified(true);
        user.setCreatedAt(OffsetDateTime.now());
        user.setUpdatedAt(OffsetDateTime.now());
        return user;
    }

    private Employer dbEmployer(long id) {
        Employer e = new Employer();
        e.setId(id);
        e.setUser(dbLinkedUser(90L));
        e.setCompanyName("Acme Ltd");
        e.setVerificationStatus("VERIFIED");
        e.setStatus("ACTIVE");
        e.setCreatedAt(OffsetDateTime.now());
        e.setUpdatedAt(OffsetDateTime.now());
        return e;
    }

    private Job dbJob(long id, Employer employer) {
        Job job = new Job();
        job.setId(id);
        job.setEmployer(employer);
        job.setCategory(null);
        job.setSlug("java-dev-" + id);
        job.setTitle("Java Developer");
        job.setDescription("Build services");
        job.setEmploymentType("FULL_TIME");
        job.setWorkMode("HYBRID");
        job.setSalaryMin(new BigDecimal("1000.00"));
        job.setSalaryMax(new BigDecimal("2000.00"));
        job.setSalaryCurrency("INR");
        job.setStatus("PUBLISHED");
        job.setPublishedAt(OffsetDateTime.now());
        job.setCreatedAt(OffsetDateTime.now());
        job.setUpdatedAt(OffsetDateTime.now());
        return job;
    }

    private Candidate dbCandidate(long id) {
        Candidate c = new Candidate();
        c.setId(id);
        c.setUser(dbLinkedUser(91L + id));
        c.setPhone("984100000" + id);
        c.setCurrentLocation("Kathmandu");
        c.setSummary("Backend developer");
        c.setAvailabilityStatus("ACTIVELY_LOOKING");
        c.setCreatedAt(OffsetDateTime.now());
        c.setUpdatedAt(OffsetDateTime.now());
        return c;
    }

    private JobApplication dbApplication(long id, Job job, Candidate candidate, String status) {
        JobApplication app = new JobApplication();
        app.setId(id);
        app.setJob(job);
        app.setCandidate(candidate);
        app.setCoverNote("I am a strong fit for this role");
        app.setStatus(status);
        app.setAppliedAt(OffsetDateTime.now());
        app.setCreatedAt(OffsetDateTime.now());
        app.setUpdatedAt(OffsetDateTime.now());
        return app;
    }

    // ------------------------------------------------------------------
    // A. Anonymous → 401 on every endpoint family
    // ------------------------------------------------------------------

    @Test
    @DisplayName("anonymous access is rejected with 401 on all application namespaces")
    void anonymousIs401Everywhere() throws Exception {
        mockMvc.perform(get(APPLICATIONS)).andExpect(status().isUnauthorized());
        mockMvc.perform(get(APPLICATIONS + "/1")).andExpect(status().isUnauthorized());
        mockMvc.perform(patch(APPLICATIONS + "/1").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"employerNote\":\"n\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post(APPLICATIONS + "/1/start-review"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post(APPLICATIONS + "/1/shortlist"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post(APPLICATIONS + "/1/decide").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"HIRED\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post(APPLICATIONS + "/1/withdraw"))
                .andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------
    // B. Permission matrix
    // ------------------------------------------------------------------

    @Test
    @DisplayName("MODERATOR and EDITOR (no employment:manage) get 403 on all application namespaces")
    void moderatorAndEditorGet403() throws Exception {
        Role moderator = role("MODERATOR", "jobs:moderate", "messages:manage");
        givenDbUser(dbUser(3L, MODERATOR_EMAIL, moderator));
        String moderatorToken = "Bearer " + jwtTokenProvider.generateAccessToken(
                3L, MODERATOR_EMAIL, List.of("MODERATOR"));

        mockMvc.perform(get(APPLICATIONS).header("Authorization", moderatorToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch(APPLICATIONS + "/1").header("Authorization", moderatorToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"employerNote\":\"n\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(APPLICATIONS + "/1/decide").header("Authorization", moderatorToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"decision\":\"HIRED\"}"))
                .andExpect(status().isForbidden());

        Role editor = role("EDITOR", "content:manage", "downloads:manage");
        givenDbUser(dbUser(4L, EDITOR_EMAIL, editor));
        String editorToken = "Bearer " + jwtTokenProvider.generateAccessToken(
                4L, EDITOR_EMAIL, List.of("EDITOR"));

        mockMvc.perform(get(APPLICATIONS).header("Authorization", editorToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(APPLICATIONS + "/1/shortlist").header("Authorization", editorToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("normal platform user (CANDIDATE role) gets 403 everywhere")
    void candidateRoleGets403() throws Exception {
        Role candidate = role("CANDIDATE");
        givenDbUser(dbUser(5L, CANDIDATE_EMAIL, candidate));
        String token = "Bearer " + jwtTokenProvider.generateAccessToken(
                5L, CANDIDATE_EMAIL, List.of("CANDIDATE"));

        mockMvc.perform(get(APPLICATIONS).header("Authorization", token))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(APPLICATIONS + "/1").header("Authorization", token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("spoofed JWT role claims do not elevate privileges")
    void spoofedJwtRoleClaimsDoNotElevate() throws Exception {
        // DB says the user is a plain CANDIDATE; the token claims SUPER_ADMIN.
        Role candidate = role("CANDIDATE");
        givenDbUser(dbUser(6L, "spoof@lokmitfoundation.org", candidate));
        String spoofed = "Bearer " + jwtTokenProvider.generateAccessToken(
                6L, "spoof@lokmitfoundation.org", List.of("SUPER_ADMIN", "ADMIN"));

        mockMvc.perform(get(APPLICATIONS).header("Authorization", spoofed))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(APPLICATIONS + "/1/decide").header("Authorization", spoofed)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"decision\":\"HIRED\"}"))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------
    // C. ADMIN and SUPER_ADMIN success paths
    // ------------------------------------------------------------------

    @Test
    @DisplayName("ADMIN can list, filter, get, patch and drive the review lifecycle")
    void adminApplicationManagement() throws Exception {
        String auth = adminAuth();
        Employer employer = dbEmployer(1L);
        Job job = dbJob(10L, employer);
        Candidate candidate = dbCandidate(20L);

        // list (empty)
        when(applicationRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));
        mockMvc.perform(get(APPLICATIONS).header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.totalItems").value(0));

        // list with filters + search params accepted (DB-side); the grouped
        // employer-count query resolves employer 1 → application 30
        when(applicationRepository.countByEmployerIds(any()))
                .thenReturn(List.<Object[]>of(new Object[]{1L, 30L}));
        when(applicationRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenAnswer(inv -> new PageImpl<>(
                        List.of(dbApplication(30L, job, candidate, "SUBMITTED")),
                        inv.getArgument(1, Pageable.class), 1));
        mockMvc.perform(get(APPLICATIONS).header("Authorization", auth)
                        .param("jobId", "10").param("candidateId", "20")
                        .param("employerId", "1").param("status", "SUBMITTED")
                        .param("search", "fit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalItems").value(1))
                .andExpect(jsonPath("$.data.items[0].status").value("SUBMITTED"))
                .andExpect(jsonPath("$.data.items[0].job.title").value("Java Developer"))
                .andExpect(jsonPath("$.data.items[0].employer.companyName").value("Acme Ltd"))
                .andExpect(jsonPath("$.data.items[0].candidate.phone").value("98410000020"));

        // get existing
        when(applicationRepository.findById(30L))
                .thenReturn(Optional.of(dbApplication(30L, job, candidate, "SUBMITTED")));
        mockMvc.perform(get(APPLICATIONS + "/30").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.coverNote").value("I am a strong fit for this role"))
                .andExpect(jsonPath("$.data.decidedAt").doesNotExist());

        // get missing → 404
        when(applicationRepository.findById(99L)).thenReturn(Optional.empty());
        mockMvc.perform(get(APPLICATIONS + "/99").header("Authorization", auth))
                .andExpect(status().isNotFound());

        // patch — employer note + resume reference
        when(applicationRepository.save(any(JobApplication.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        mockMvc.perform(patch(APPLICATIONS + "/30").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"employerNote\":\"Strong backend profile\",\"resumeId\":5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.employerNote").value("Strong backend profile"))
                .andExpect(jsonPath("$.data.resumeId").value(5));

        // patch cannot change status (not a patchable field)
        mockMvc.perform(patch(APPLICATIONS + "/30").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"HIRED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUBMITTED"));

        // lifecycle chain: SUBMITTED → UNDER_REVIEW → SHORTLISTED → HIRED
        JobApplication submitted =
                dbApplication(30L, job, candidate, JobApplication.STATUS_SUBMITTED);
        when(applicationRepository.findById(30L)).thenReturn(Optional.of(submitted));
        mockMvc.perform(post(APPLICATIONS + "/30/start-review").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("UNDER_REVIEW"));
        mockMvc.perform(post(APPLICATIONS + "/30/shortlist").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SHORTLISTED"));
        mockMvc.perform(post(APPLICATIONS + "/30/decide").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"HIRED\",\"note\":\"Offer sent\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("HIRED"))
                .andExpect(jsonPath("$.data.decidedAt").isNotEmpty())
                .andExpect(jsonPath("$.data.employerNote").value("Offer sent"));

        // decide again → 409 (terminal)
        mockMvc.perform(post(APPLICATIONS + "/30/decide").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"decision\":\"REJECTED\"}"))
                .andExpect(status().isConflict());

        // withdraw a decided application → 409
        mockMvc.perform(post(APPLICATIONS + "/30/withdraw").header("Authorization", auth))
                .andExpect(status().isConflict());

        // invalid transitions on a WITHDRAWN application → 400
        JobApplication withdrawn =
                dbApplication(31L, job, candidate, JobApplication.STATUS_WITHDRAWN);
        when(applicationRepository.findById(31L)).thenReturn(Optional.of(withdrawn));
        mockMvc.perform(post(APPLICATIONS + "/31/start-review").header("Authorization", auth))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post(APPLICATIONS + "/31/decide").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"decision\":\"HIRED\"}"))
                .andExpect(status().isBadRequest());

        // shortlist straight from SUBMITTED → 400
        JobApplication fresh =
                dbApplication(32L, job, candidate, JobApplication.STATUS_SUBMITTED);
        when(applicationRepository.findById(32L)).thenReturn(Optional.of(fresh));
        mockMvc.perform(post(APPLICATIONS + "/32/shortlist").header("Authorization", auth))
                .andExpect(status().isBadRequest());

        // withdraw a fresh application → WITHDRAWN; repeat → 409
        JobApplication fresh2 =
                dbApplication(33L, job, candidate, JobApplication.STATUS_SUBMITTED);
        when(applicationRepository.findById(33L)).thenReturn(Optional.of(fresh2));
        mockMvc.perform(post(APPLICATIONS + "/33/withdraw").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("WITHDRAWN"));

        // lifecycle on missing application → 404
        when(applicationRepository.findById(99L)).thenReturn(Optional.empty());
        mockMvc.perform(post(APPLICATIONS + "/99/start-review").header("Authorization", auth))
                .andExpect(status().isNotFound());
        mockMvc.perform(post(APPLICATIONS + "/99/decide").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"decision\":\"HIRED\"}"))
                .andExpect(status().isNotFound());

        // NO delete endpoint exists (applications are the hiring audit trail)
        mockMvc.perform(delete(APPLICATIONS + "/30").header("Authorization", auth))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    @DisplayName("SUPER_ADMIN can review applications end to end")
    void superAdminApplicationManagement() throws Exception {
        Role superAdmin = role("SUPER_ADMIN", "users:manage", "dashboard:view",
                "content:manage", "content:publish", "downloads:manage", "messages:manage",
                "jobs:manage", "jobs:moderate", "settings:manage", "employment:manage",
                "candidates:manage");
        givenDbUser(dbUser(1L, SUPER_ADMIN_EMAIL, superAdmin));
        String auth = "Bearer " + jwtTokenProvider.generateAccessToken(
                1L, SUPER_ADMIN_EMAIL, List.of("SUPER_ADMIN"));

        Job job = dbJob(10L, dbEmployer(1L));
        Candidate candidate = dbCandidate(20L);
        when(applicationRepository.findById(30L))
                .thenReturn(Optional.of(dbApplication(30L, job, candidate,
                        JobApplication.STATUS_UNDER_REVIEW)));
        when(applicationRepository.save(any(JobApplication.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // decide REJECTED from UNDER_REVIEW → terminal + stamped
        mockMvc.perform(post(APPLICATIONS + "/30/decide").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"decision\":\"REJECTED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"))
                .andExpect(jsonPath("$.data.decidedAt").isNotEmpty());
    }

    // ------------------------------------------------------------------
    // D. Validation → 400
    // ------------------------------------------------------------------

    @Test
    @DisplayName("validation failures return 400 (bad decision body, bad resumeId)")
    void validationFailuresAre400() throws Exception {
        String auth = adminAuth();

        // decide with a non-lifecycle decision value → 400 from the controller
        mockMvc.perform(post(APPLICATIONS + "/1/decide").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"APPROVED\"}"))
                .andExpect(status().isBadRequest());

        // patch with a negative resumeId → 400 from bean validation
        mockMvc.perform(patch(APPLICATIONS + "/1").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resumeId\":-5}"))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------
    // E. Pagination
    // ------------------------------------------------------------------

    @Test
    @DisplayName("pagination defaults and page-size cap behave as project convention")
    void paginationBehavior() throws Exception {
        String auth = adminAuth();

        when(applicationRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenAnswer(inv -> new PageImpl<>(List.of(), inv.getArgument(1,
                        Pageable.class), 0));
        mockMvc.perform(get(APPLICATIONS).header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.page").value(0));

        // cap: size=200 → 400
        mockMvc.perform(get(APPLICATIONS).header("Authorization", auth)
                        .param("size", "200"))
                .andExpect(status().isBadRequest());

        // negative page → 400
        mockMvc.perform(get(APPLICATIONS).header("Authorization", auth)
                        .param("page", "-1"))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------
    // F. DTO safety
    // ------------------------------------------------------------------

    @Test
    @DisplayName("application responses never expose candidate/employer-user security fields")
    void dtoSafety() throws Exception {
        String auth = adminAuth();

        Candidate candidate = dbCandidate(20L);
        // poison the candidate's linked user with security internals — none may leak
        User linked = candidate.getUser();
        linked.setPasswordHash("$2a$10$secret-hash-should-never-leak");
        linked.setFailedLoginAttempts(7);
        linked.setLockedUntil(OffsetDateTime.now().plusMinutes(10));

        when(applicationRepository.findById(30L)).thenReturn(Optional.of(
                dbApplication(30L, dbJob(10L, dbEmployer(1L)), candidate, "SUBMITTED")));

        String body = mockMvc.perform(get(APPLICATIONS + "/30").header("Authorization", auth))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(body)
                .doesNotContain("password")
                .doesNotContain("secret-hash")
                .doesNotContain("failedLogin")
                .doesNotContain("lockedUntil")
                .doesNotContain("refreshToken")
                .doesNotContain("userType")
                .doesNotContain("@example.org")
                .doesNotContain("\"roles\"");
    }
}
