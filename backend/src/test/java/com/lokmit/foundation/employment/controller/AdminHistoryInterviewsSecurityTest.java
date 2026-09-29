package com.lokmit.foundation.employment.controller;

import com.lokmit.foundation.audit.repository.AuditLogRepository;
import com.lokmit.foundation.audit.service.AuditLogService;
import com.lokmit.foundation.common.constants.ApiPaths;
import com.lokmit.foundation.employment.application.controller.ApplicationController;
import com.lokmit.foundation.employment.application.entity.JobApplication;
import com.lokmit.foundation.employment.application.history.controller.ApplicationHistoryController;
import com.lokmit.foundation.employment.application.history.dto.ApplicationStatusHistoryResponse;
import com.lokmit.foundation.employment.application.history.entity.ApplicationStatusHistory;
import com.lokmit.foundation.employment.application.history.repository.ApplicationStatusHistoryRepository;
import com.lokmit.foundation.employment.application.history.service.ApplicationStatusHistoryService;
import com.lokmit.foundation.employment.application.interview.controller.InterviewController;
import com.lokmit.foundation.employment.application.interview.dto.InterviewResponse;
import com.lokmit.foundation.employment.application.interview.entity.Interview;
import com.lokmit.foundation.employment.application.interview.repository.InterviewRepository;
import com.lokmit.foundation.employment.application.interview.service.InterviewService;
import com.lokmit.foundation.notification.repository.NotificationRepository;
import com.lokmit.foundation.outbox.config.OutboxRelayConfig;
import com.lokmit.foundation.outbox.repository.OutboxEventRepository;
import com.lokmit.foundation.outbox.service.OutboxEventProcessor;
import com.lokmit.foundation.outbox.service.OutboxService;
import com.lokmit.foundation.employment.application.repository.JobApplicationRepository;
import com.lokmit.foundation.employment.application.service.ApplicationService;
import com.lokmit.foundation.employment.candidate.entity.Candidate;
import com.lokmit.foundation.employment.candidate.repository.CandidateRepository;
import com.lokmit.foundation.employment.employer.entity.Employer;
import com.lokmit.foundation.employment.job.entity.Job;
import com.lokmit.foundation.employment.job.repository.JobRepository;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
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
 * A7.4 security and behavior matrix (application status history + interviews)
 * exercised through the REAL Spring Security filter chain (real
 * JwtAuthenticationFilter, real JwtTokenProvider, real
 * CustomUserDetailsService with mocked repositories — identical to the
 * A1–A7.3 matrix setups).
 *
 * <p>Coverage: anonymous 401 on the history/interview namespaces; role matrix
 * (MODERATOR/EDITOR/CANDIDATE-role 403; spoofed SUPER_ADMIN claim 403; ADMIN
 * and SUPER_ADMIN success); history rows written by lifecycle transitions
 * inside the same service call (previous/new statuses, actor from the
 * authenticated principal, ordering, pagination, DTO safety); interview
 * CRUD with terminal-application rejection, cross-application 404 masking,
 * conservative status transitions, CANCELLED-only delete, and offset
 * preservation; A7.3 lifecycle regression.</p>
 */
@WebMvcTest(controllers = {ApplicationController.class,
        ApplicationHistoryController.class, InterviewController.class})
@Import({SecurityConfig.class, CorsConfig.class, JwtKeyConfig.class,
        JwtTokenProvider.class, CustomUserDetailsService.class, SecurityUtils.class,
        ApplicationService.class, ApplicationStatusHistoryService.class,
        InterviewService.class, AuditLogService.class, OutboxService.class,
        OutboxEventProcessor.class, OutboxRelayConfig.class})
@AutoConfigureMockMvc
class AdminHistoryInterviewsSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JobApplicationRepository applicationRepository;

    @MockitoBean
    private ApplicationStatusHistoryRepository historyRepository;

    @MockitoBean
    private InterviewRepository interviewRepository;

    @MockitoBean
    private AuditLogRepository auditLogRepository;

    @MockitoBean
    private NotificationRepository notificationRepository;

    @MockitoBean
    private OutboxEventRepository outboxEventRepository;

    @MockitoBean
    private JobRepository jobRepository;

    @MockitoBean
    private CandidateRepository candidateRepository;

    @MockitoBean
    private UserRepository userRepository;

    // A7.6.6: ApplicationService now cross-checks resume ownership in
    // updateReview; the service bean is imported here, so its repository
    // dependency must be present in the slice.
    @MockitoBean
    private com.lokmit.foundation.employment.resume.repository.ResumeRepository resumeRepository;

    private static final String APPLICATIONS = ApiPaths.ADMIN_APPLICATIONS;
    private static final String HISTORY = APPLICATIONS + "/{id}/history";
    private static final String INTERVIEWS = APPLICATIONS + "/{id}/interviews";
    private static final String INTERVIEW = INTERVIEWS + "/{interviewId}";

    private static final String SUPER_ADMIN_EMAIL = "superadmin@lokmitfoundation.org";
    private static final String ADMIN_EMAIL = "admin@lokmitfoundation.org";
    private static final String MODERATOR_EMAIL = "moderator@lokmitfoundation.org";
    private static final String EDITOR_EMAIL = "editor@lokmitfoundation.org";
    private static final String CANDIDATE_EMAIL = "candidate@lokmitfoundation.org";

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void resetStubs() {
        reset(applicationRepository, historyRepository, interviewRepository,
                auditLogRepository, notificationRepository, outboxEventRepository,
                jobRepository, candidateRepository, userRepository, resumeRepository);
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

    private JobApplication dbApplication(long id, String status) {
        JobApplication app = new JobApplication();
        app.setId(id);
        app.setJob(dbJob(10L, dbEmployer(1L)));
        app.setCandidate(dbCandidate(20L));
        app.setCoverNote("I am a strong fit for this role");
        app.setStatus(status);
        app.setAppliedAt(OffsetDateTime.now());
        app.setCreatedAt(OffsetDateTime.now());
        app.setUpdatedAt(OffsetDateTime.now());
        return app;
    }

    private Interview dbInterview(long id, long applicationId, String status,
                                  OffsetDateTime scheduledAt) {
        Interview interview = new Interview();
        interview.setId(id);
        interview.setApplication(dbApplication(applicationId, "UNDER_REVIEW"));
        interview.setScheduledAt(scheduledAt);
        interview.setMode(Interview.MODE_REMOTE);
        interview.setLocation("https://meet.example.org/room");
        interview.setStatus(status);
        interview.setNotes("Panel interview");
        interview.setCreatedAt(OffsetDateTime.now());
        interview.setUpdatedAt(OffsetDateTime.now());
        return interview;
    }

    private ApplicationStatusHistory dbHistory(long id, long applicationId,
                                               String previous, String next) {
        ApplicationStatusHistory h = new ApplicationStatusHistory();
        h.setId(id);
        h.setApplication(dbApplication(applicationId, next));
        h.setPreviousStatus(previous);
        h.setNewStatus(next);
        h.setChangedBy(2L);
        h.setChangedAt(OffsetDateTime.now());
        h.setNote("review note");
        return h;
    }

    // ------------------------------------------------------------------
    // A. Anonymous → 401 on every new endpoint
    // ------------------------------------------------------------------

    @Test
    @DisplayName("anonymous access is rejected with 401 on history and interview namespaces")
    void anonymousIs401Everywhere() throws Exception {
        mockMvc.perform(get(APPLICATIONS + "/5/history"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get(APPLICATIONS + "/5/interviews"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post(APPLICATIONS + "/5/interviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scheduledAt\":\"2026-10-01T10:30:00+05:45\",\"mode\":\"ONSITE\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get(APPLICATIONS + "/5/interviews/7"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(patch(APPLICATIONS + "/5/interviews/7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"COMPLETED\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete(APPLICATIONS + "/5/interviews/7"))
                .andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------
    // B. Permission matrix
    // ------------------------------------------------------------------

    @Test
    @DisplayName("MODERATOR, EDITOR and CANDIDATE-role users get 403 on history and interviews")
    void unauthorizedRolesGet403() throws Exception {
        Role moderator = role("MODERATOR", "jobs:moderate", "messages:manage");
        givenDbUser(dbUser(3L, MODERATOR_EMAIL, moderator));
        String moderatorToken = "Bearer " + jwtTokenProvider.generateAccessToken(
                3L, MODERATOR_EMAIL, List.of("MODERATOR"));

        mockMvc.perform(get(APPLICATIONS + "/5/history").header("Authorization", moderatorToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(APPLICATIONS + "/5/interviews").header("Authorization", moderatorToken))
                .andExpect(status().isForbidden());

        Role editor = role("EDITOR", "content:manage", "downloads:manage");
        givenDbUser(dbUser(4L, EDITOR_EMAIL, editor));
        String editorToken = "Bearer " + jwtTokenProvider.generateAccessToken(
                4L, EDITOR_EMAIL, List.of("EDITOR"));
        mockMvc.perform(post(APPLICATIONS + "/5/interviews").header("Authorization", editorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scheduledAt\":\"2026-10-01T10:30:00+05:45\",\"mode\":\"ONSITE\"}"))
                .andExpect(status().isForbidden());

        Role candidate = role("CANDIDATE");
        givenDbUser(dbUser(5L, CANDIDATE_EMAIL, candidate));
        String candidateToken = "Bearer " + jwtTokenProvider.generateAccessToken(
                5L, CANDIDATE_EMAIL, List.of("CANDIDATE"));
        mockMvc.perform(get(APPLICATIONS + "/5/interviews/7")
                        .header("Authorization", candidateToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("spoofed JWT role claims do not elevate privileges on A7.4 endpoints")
    void spoofedJwtRoleClaimsDoNotElevate() throws Exception {
        Role candidate = role("CANDIDATE");
        givenDbUser(dbUser(6L, "spoof@lokmitfoundation.org", candidate));
        String spoofed = "Bearer " + jwtTokenProvider.generateAccessToken(
                6L, "spoof@lokmitfoundation.org", List.of("SUPER_ADMIN", "ADMIN"));

        mockMvc.perform(get(APPLICATIONS + "/5/history").header("Authorization", spoofed))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(APPLICATIONS + "/5/interviews").header("Authorization", spoofed)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scheduledAt\":\"2026-10-01T10:30:00+05:45\",\"mode\":\"ONSITE\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete(APPLICATIONS + "/5/interviews/7")
                        .header("Authorization", spoofed))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------
    // C. History behavior
    // ------------------------------------------------------------------

    @Test
    @DisplayName("lifecycle transitions write history rows with correct previous/new statuses and actor")
    void lifecycleTransitionsRecordHistory() throws Exception {
        String auth = adminAuth();
        // adminAuth() registers DB user id 2 — the recorded actor must be 2.

        JobApplication submitted = dbApplication(30L, JobApplication.STATUS_SUBMITTED);
        when(applicationRepository.findById(30L)).thenReturn(Optional.of(submitted));
        when(applicationRepository.save(any(JobApplication.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(post(APPLICATIONS + "/30/start-review").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("UNDER_REVIEW"));

        // History insert captured: SUBMITTED → UNDER_REVIEW by actor 2
        var historyCaptor = org.mockito.ArgumentCaptor
                .forClass(ApplicationStatusHistory.class);
        verify(historyRepository).save(historyCaptor.capture());
        ApplicationStatusHistory recorded = historyCaptor.getValue();
        assertThat(recorded.getPreviousStatus()).isEqualTo("SUBMITTED");
        assertThat(recorded.getNewStatus()).isEqualTo("UNDER_REVIEW");
        assertThat(recorded.getChangedBy()).isEqualTo(2L);
        assertThat(recorded.getChangedAt()).isNotNull();

        // decide with note → note carried into the history row
        org.mockito.Mockito.clearInvocations(historyRepository);
        JobApplication underReview = dbApplication(30L, JobApplication.STATUS_UNDER_REVIEW);
        when(applicationRepository.findById(30L)).thenReturn(Optional.of(underReview));
        mockMvc.perform(post(APPLICATIONS + "/30/decide").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"REJECTED\",\"note\":\"Not enough experience\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"));

        verify(historyRepository).save(historyCaptor.capture());
        ApplicationStatusHistory decided = historyCaptor.getValue();
        assertThat(decided.getPreviousStatus()).isEqualTo("UNDER_REVIEW");
        assertThat(decided.getNewStatus()).isEqualTo("REJECTED");
        assertThat(decided.getNote()).isEqualTo("Not enough experience");
        assertThat(decided.getChangedBy()).isEqualTo(2L);
    }

    @Test
    @DisplayName("ADMIN and SUPER_ADMIN can read history; unknown application is 404; rows are ordered and paginated")
    void historyReadingMatrix() throws Exception {
        String auth = adminAuth();

        when(applicationRepository.existsById(30L)).thenReturn(true);
        when(historyRepository.findByApplicationIdOrderByChangedAtDesc(
                org.mockito.ArgumentMatchers.eq(30L), any(Pageable.class)))
                .thenAnswer(inv -> new PageImpl<>(
                        List.of(
                                toDto(101L, "UNDER_REVIEW", "SHORTLISTED"),
                                toDto(100L, "SUBMITTED", "UNDER_REVIEW")),
                        inv.getArgument(1, Pageable.class), 2));

        mockMvc.perform(get(APPLICATIONS + "/30/history")
                        .header("Authorization", auth).param("page", "0").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.items[0].previousStatus").value("UNDER_REVIEW"))
                .andExpect(jsonPath("$.data.items[0].newStatus").value("SHORTLISTED"))
                .andExpect(jsonPath("$.data.items[1].previousStatus").value("SUBMITTED"))
                .andExpect(jsonPath("$.data.totalItems").value(2));

        // unknown application → 404 (no existence leak of any history)
        when(applicationRepository.existsById(99L)).thenReturn(false);
        mockMvc.perform(get(APPLICATIONS + "/99/history").header("Authorization", auth))
                .andExpect(status().isNotFound());

        // pagination cap → 400
        mockMvc.perform(get(APPLICATIONS + "/30/history")
                        .header("Authorization", auth).param("size", "200"))
                .andExpect(status().isBadRequest());
    }

    private ApplicationStatusHistory toDto(long id, String previous, String next) {
        ApplicationStatusHistory h = new ApplicationStatusHistory();
        h.setId(id);
        h.setApplication(dbApplication(30L, next));
        h.setPreviousStatus(previous);
        h.setNewStatus(next);
        h.setChangedBy(2L);
        h.setChangedAt(OffsetDateTime.now());
        return h;
    }

    // ------------------------------------------------------------------
    // D. Interview behavior
    // ------------------------------------------------------------------

    @Test
    @DisplayName("SUPER_ADMIN can schedule, list, read, patch and delete interviews end to end")
    void superAdminInterviewLifecycle() throws Exception {
        Role superAdmin = role("SUPER_ADMIN", "users:manage", "dashboard:view",
                "content:manage", "content:publish", "downloads:manage", "messages:manage",
                "jobs:manage", "jobs:moderate", "settings:manage", "employment:manage",
                "candidates:manage");
        givenDbUser(dbUser(1L, SUPER_ADMIN_EMAIL, superAdmin));
        String auth = "Bearer " + jwtTokenProvider.generateAccessToken(
                1L, SUPER_ADMIN_EMAIL, List.of("SUPER_ADMIN"));

        // create
        JobApplication app = dbApplication(30L, JobApplication.STATUS_UNDER_REVIEW);
        when(applicationRepository.findById(30L)).thenReturn(Optional.of(app));
        when(interviewRepository.save(any(Interview.class))).thenAnswer(inv -> {
            Interview i = inv.getArgument(0);
            i.setId(500L);
            return i;
        });

        mockMvc.perform(post(APPLICATIONS + "/30/interviews").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scheduledAt\":\"2026-10-01T10:30:00+05:45\","
                                + "\"mode\":\"ONSITE\",\"location\":\"Kathmandu office\","
                                + "\"notes\":\"Bring portfolio\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SCHEDULED"))
                .andExpect(jsonPath("$.data.mode").value("ONSITE"))
                // the +05:45 offset instant is preserved exactly (10:30+05:45 ==
                // 04:45Z); response serialization follows the project-wide
                // Jackson convention of normalizing to UTC.
                .andExpect(jsonPath("$.data.scheduledAt").value("2026-10-01T04:45:00Z"));

        // create cannot set status from the client (unknown field ignored;
        // status always starts SCHEDULED)
        mockMvc.perform(post(APPLICATIONS + "/30/interviews").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scheduledAt\":\"2026-10-02T10:30:00+05:45\","
                                + "\"mode\":\"REMOTE\",\"status\":\"COMPLETED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SCHEDULED"));

        // list
        when(interviewRepository.findByApplicationIdOrderByScheduledAtDesc(
                org.mockito.ArgumentMatchers.eq(30L), any(Pageable.class)))
                .thenAnswer(inv -> new PageImpl<>(
                        List.of(dbInterview(500L, 30L, Interview.STATUS_SCHEDULED,
                                OffsetDateTime.parse("2026-10-01T10:30+05:45"))),
                        inv.getArgument(1, Pageable.class), 1));
        mockMvc.perform(get(APPLICATIONS + "/30/interviews").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalItems").value(1))
                .andExpect(jsonPath("$.data.items[0].mode").value("REMOTE"));

        // get
        when(interviewRepository.findByIdAndApplicationId(500L, 30L))
                .thenReturn(Optional.of(dbInterview(500L, 30L,
                        Interview.STATUS_SCHEDULED,
                        OffsetDateTime.parse("2026-10-01T10:30+05:45"))));
        mockMvc.perform(get(APPLICATIONS + "/30/interviews/500").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SCHEDULED"));

        // patch: reschedule + transition SCHEDULED → COMPLETED
        Interview completable = dbInterview(500L, 30L, Interview.STATUS_SCHEDULED,
                OffsetDateTime.parse("2026-10-01T10:30+05:45"));
        when(interviewRepository.findByIdAndApplicationId(500L, 30L))
                .thenReturn(Optional.of(completable));
        mockMvc.perform(patch(APPLICATIONS + "/30/interviews/500").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scheduledAt\":\"2026-10-05T14:00:00+05:45\","
                                + "\"status\":\"COMPLETED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.scheduledAt").value("2026-10-05T08:15:00Z"));
        // same instant as 14:00+05:45 — offset normalization only, per project convention

        // patch out of a terminal state → 409
        mockMvc.perform(patch(APPLICATIONS + "/30/interviews/500").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"SCHEDULED\"}"))
                .andExpect(status().isConflict());

        // cancel then delete (only CANCELLED may be deleted)
        Interview cancellable = dbInterview(501L, 30L, Interview.STATUS_SCHEDULED,
                OffsetDateTime.parse("2026-10-06T10:00+05:45"));
        when(interviewRepository.findByIdAndApplicationId(501L, 30L))
                .thenReturn(Optional.of(cancellable));
        mockMvc.perform(patch(APPLICATIONS + "/30/interviews/501").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CANCELLED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
        mockMvc.perform(delete(APPLICATIONS + "/30/interviews/501").header("Authorization", auth))
                .andExpect(status().isNoContent());
        verify(interviewRepository).delete(cancellable);

        // deleting a non-cancelled interview → 409
        Interview active = dbInterview(502L, 30L, Interview.STATUS_SCHEDULED,
                OffsetDateTime.parse("2026-10-07T10:00+05:45"));
        when(interviewRepository.findByIdAndApplicationId(502L, 30L))
                .thenReturn(Optional.of(active));
        mockMvc.perform(delete(APPLICATIONS + "/30/interviews/502").header("Authorization", auth))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("terminal applications reject new interviews; cross-application access is a plain 404")
    void terminalAndCrossApplicationRules() throws Exception {
        String auth = adminAuth();

        // terminal statuses reject new interviews with 400
        for (String terminal : List.of("HIRED", "REJECTED", "WITHDRAWN")) {
            when(applicationRepository.findById(40L)).thenReturn(
                    Optional.of(dbApplication(40L, terminal)));
            mockMvc.perform(post(APPLICATIONS + "/40/interviews").header("Authorization", auth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"scheduledAt\":\"2026-10-01T10:30:00+05:45\","
                                    + "\"mode\":\"PHONE\"}"))
                    .andExpect(status().isBadRequest());
        }

        // unknown application → 404 (list + create + detail)
        when(applicationRepository.findById(99L)).thenReturn(Optional.empty());
        mockMvc.perform(get(APPLICATIONS + "/99/interviews").header("Authorization", auth))
                .andExpect(status().isNotFound());
        mockMvc.perform(post(APPLICATIONS + "/99/interviews").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scheduledAt\":\"2026-10-01T10:30:00+05:45\",\"mode\":\"PHONE\"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get(APPLICATIONS + "/99/interviews/7").header("Authorization", auth))
                .andExpect(status().isNotFound());

        // interview belongs to another application → plain 404 (no leak)
        when(applicationRepository.findById(30L))
                .thenReturn(Optional.of(dbApplication(30L, JobApplication.STATUS_UNDER_REVIEW)));
        when(interviewRepository.findByIdAndApplicationId(7L, 30L))
                .thenReturn(Optional.empty());
        mockMvc.perform(get(APPLICATIONS + "/30/interviews/7").header("Authorization", auth))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete(APPLICATIONS + "/30/interviews/7").header("Authorization", auth))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("interview validation failures return 400 (bad mode, bad status, malformed datetime)")
    void interviewValidationFailures() throws Exception {
        String auth = adminAuth();
        when(applicationRepository.findById(30L))
                .thenReturn(Optional.of(dbApplication(30L, JobApplication.STATUS_UNDER_REVIEW)));

        // invalid mode → 400
        mockMvc.perform(post(APPLICATIONS + "/30/interviews").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scheduledAt\":\"2026-10-01T10:30:00+05:45\",\"mode\":\"VIDEO\"}"))
                .andExpect(status().isBadRequest());

        // missing scheduledAt → 400
        mockMvc.perform(post(APPLICATIONS + "/30/interviews").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mode\":\"ONSITE\"}"))
                .andExpect(status().isBadRequest());

        // malformed date-time → 400 (Jackson)
        mockMvc.perform(post(APPLICATIONS + "/30/interviews").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scheduledAt\":\"not-a-date\",\"mode\":\"ONSITE\"}"))
                .andExpect(status().isBadRequest());

        // patch with invalid status value → 400
        when(interviewRepository.findByIdAndApplicationId(500L, 30L))
                .thenReturn(Optional.of(dbInterview(500L, 30L, Interview.STATUS_SCHEDULED,
                        OffsetDateTime.parse("2026-10-01T10:30+05:45"))));
        mockMvc.perform(patch(APPLICATIONS + "/30/interviews/500").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"RESCHEDULED\"}"))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------
    // E. Pagination + DTO safety + A7.3 regression
    // ------------------------------------------------------------------

    @Test
    @DisplayName("interview list honors pagination defaults and cap")
    void interviewPaginationBehavior() throws Exception {
        String auth = adminAuth();
        when(applicationRepository.findById(30L))
                .thenReturn(Optional.of(dbApplication(30L, JobApplication.STATUS_UNDER_REVIEW)));
        when(interviewRepository.findByApplicationIdOrderByScheduledAtDesc(
                org.mockito.ArgumentMatchers.eq(30L), any(Pageable.class)))
                .thenAnswer(inv -> new PageImpl<>(List.of(),
                        inv.getArgument(1, Pageable.class), 0));

        mockMvc.perform(get(APPLICATIONS + "/30/interviews").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(20));

        mockMvc.perform(get(APPLICATIONS + "/30/interviews")
                        .header("Authorization", auth).param("size", "150"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("history and interview responses never expose security fields")
    void dtoSafety() throws Exception {
        String auth = adminAuth();

        // history service checks existsById; interview service loads findById
        when(applicationRepository.existsById(30L)).thenReturn(true);
        when(applicationRepository.findById(30L))
                .thenReturn(Optional.of(dbApplication(30L, JobApplication.STATUS_UNDER_REVIEW)));
        when(historyRepository.findByApplicationIdOrderByChangedAtDesc(
                org.mockito.ArgumentMatchers.eq(30L), any(Pageable.class)))
                .thenAnswer(inv -> new PageImpl<>(List.of(
                        toDto(100L, "SUBMITTED", "UNDER_REVIEW")),
                        inv.getArgument(1, Pageable.class), 1));

        String historyBody = mockMvc.perform(
                        get(APPLICATIONS + "/30/history").header("Authorization", auth))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(historyBody)
                .doesNotContain("password")
                .doesNotContain("token")
                .doesNotContain("failedLogin")
                .doesNotContain("lockedUntil")
                .doesNotContain("@lokmitfoundation.org");

        when(interviewRepository.findByApplicationIdOrderByScheduledAtDesc(
                org.mockito.ArgumentMatchers.eq(30L), any(Pageable.class)))
                .thenAnswer(inv -> new PageImpl<>(List.of(
                        dbInterview(500L, 30L, Interview.STATUS_SCHEDULED,
                                OffsetDateTime.parse("2026-10-01T10:30+05:45"))),
                        inv.getArgument(1, Pageable.class), 1));
        String interviewBody = mockMvc.perform(
                        get(APPLICATIONS + "/30/interviews").header("Authorization", auth))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(interviewBody)
                .doesNotContain("password")
                .doesNotContain("token")
                .doesNotContain("userType")
                .doesNotContain("@example.org");
    }

    @Test
    @DisplayName("A7.3 regression: review lifecycle and withdraw still behave with history integration")
    void a73LifecycleRegression() throws Exception {
        String auth = adminAuth();

        // withdraw from SUBMITTED → WITHDRAWN + history
        JobApplication fresh = dbApplication(33L, JobApplication.STATUS_SUBMITTED);
        when(applicationRepository.findById(33L)).thenReturn(Optional.of(fresh));
        when(applicationRepository.save(any(JobApplication.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        mockMvc.perform(post(APPLICATIONS + "/33/withdraw").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("WITHDRAWN"));

        // repeat withdraw → 409
        mockMvc.perform(post(APPLICATIONS + "/33/withdraw").header("Authorization", auth))
                .andExpect(status().isConflict());

        // shortlist straight from SUBMITTED → 400 (unchanged semantics)
        JobApplication fresh2 = dbApplication(34L, JobApplication.STATUS_SUBMITTED);
        when(applicationRepository.findById(34L)).thenReturn(Optional.of(fresh2));
        mockMvc.perform(post(APPLICATIONS + "/34/shortlist").header("Authorization", auth))
                .andExpect(status().isBadRequest());

        // PATCH still cannot change status; identity immutable (no endpoint
        // accepts job/candidate reassignment)
        JobApplication submitted = dbApplication(30L, JobApplication.STATUS_SUBMITTED);
        when(applicationRepository.findById(30L)).thenReturn(Optional.of(submitted));
        mockMvc.perform(patch(APPLICATIONS + "/30").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"HIRED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUBMITTED"));
    }
}
