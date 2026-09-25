package com.lokmit.foundation.employment.controller;

import com.lokmit.foundation.audit.repository.AuditLogRepository;
import com.lokmit.foundation.audit.service.AuditLogService;
import com.lokmit.foundation.common.constants.ApiPaths;
import com.lokmit.foundation.employment.application.controller.ApplicationController;
import com.lokmit.foundation.employment.application.controller.CandidateApplicationController;
import com.lokmit.foundation.employment.application.entity.JobApplication;
import com.lokmit.foundation.employment.application.history.repository.ApplicationStatusHistoryRepository;
import com.lokmit.foundation.employment.application.history.service.ApplicationStatusHistoryService;
import com.lokmit.foundation.employment.application.repository.JobApplicationRepository;
import com.lokmit.foundation.employment.application.service.ApplicationService;
import com.lokmit.foundation.employment.application.service.CandidateApplicationService;
import com.lokmit.foundation.employment.candidate.repository.CandidateRepository;
import com.lokmit.foundation.employment.employer.entity.Employer;
import com.lokmit.foundation.employment.job.entity.Job;
import com.lokmit.foundation.employment.job.repository.JobRepository;
import com.lokmit.foundation.employment.resume.entity.Resume;
import com.lokmit.foundation.employment.resume.repository.ResumeRepository;
import com.lokmit.foundation.employment.resume.service.ResumeOwnershipService;
import com.lokmit.foundation.notification.repository.NotificationRepository;
import com.lokmit.foundation.outbox.config.OutboxRelayConfig;
import com.lokmit.foundation.outbox.repository.OutboxEventRepository;
import com.lokmit.foundation.outbox.service.OutboxService;
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
import org.mockito.Mockito;
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
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A9 security and behavior matrix exercised through the REAL Spring Security
 * filter chain (same setup as the A8 public-jobs and A7.2 admin-jobs
 * matrices).
 *
 * <p>Coverage: anonymous submission rejected 401; authenticated candidate
 * can submit; candidate identity comes only from the JWT-backed principal
 * (no candidateId field exists to spoof); only PUBLISHED jobs are
 * applicable (unknown/non-published identical 404, no existence leak);
 * resume must be OWN + ACTIVE (foreign/inactive/nonexistent masked 404);
 * duplicates 409 per the existing uq constraint; initial SUBMITTED status;
 * A7.4 history + A7.5 audit/outbox written transactionally; ownership-
 * scoped read APIs; and the A7.3 admin application APIs remain exactly as
 * protected as before.</p>
 */
@WebMvcTest(controllers = {CandidateApplicationController.class, ApplicationController.class})
@Import({SecurityConfig.class, CorsConfig.class, JwtKeyConfig.class,
        JwtTokenProvider.class, CustomUserDetailsService.class, SecurityUtils.class,
        CandidateApplicationService.class, ApplicationService.class,
        ApplicationStatusHistoryService.class, AuditLogService.class,
        ResumeOwnershipService.class, OutboxService.class, OutboxRelayConfig.class})
@AutoConfigureMockMvc
class CandidateApplicationsSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JobApplicationRepository applicationRepository;

    @MockitoBean
    private JobRepository jobRepository;

    @MockitoBean
    private ResumeRepository resumeRepository;

    @MockitoBean
    private CandidateRepository candidateRepository;

    @MockitoBean
    private ApplicationStatusHistoryRepository historyRepository;

    @MockitoBean
    private AuditLogRepository auditLogRepository;

    @MockitoBean
    private OutboxEventRepository outboxEventRepository;

    @MockitoBean
    private NotificationRepository notificationRepository;

    @MockitoBean
    private UserRepository userRepository;

    private static final String ME_APPLICATIONS = ApiPaths.CANDIDATE_ME_APPLICATIONS;
    private static final String ADMIN_APPLICATIONS = ApiPaths.ADMIN_APPLICATIONS;

    private static final String CANDIDATE_EMAIL = "candidate@lokmitfoundation.org";
    private static final String OTHER_EMAIL = "other@lokmitfoundation.org";
    private static final String ADMIN_EMAIL = "admin@lokmitfoundation.org";

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void resetStubs() {
        reset(applicationRepository, jobRepository, resumeRepository,
                candidateRepository, historyRepository, auditLogRepository,
                outboxEventRepository, notificationRepository, userRepository);
    }

    // ------------------------------------------------------------------
    // fixtures
    // ------------------------------------------------------------------

    private User dbUser(long id, String email, Role... roles) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setPasswordHash("hash-not-used-by-jwt-filter-0123456789");
        user.setFullName("Test User");
        user.setUserType("CANDIDATE");
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
        role.setPermissions(Set.of(permissionCodes).stream().map(pc -> {
            Permission p = new Permission();
            p.setCode(pc);
            return p;
        }).collect(Collectors.toSet()));
        return role;
    }

    private com.lokmit.foundation.employment.candidate.entity.Candidate candidate(long id, User user) {
        var c = new com.lokmit.foundation.employment.candidate.entity.Candidate();
        c.setId(id);
        c.setUser(user);
        c.setPhone("+91-900000000" + id);
        c.setCurrentLocation("Patna");
        c.setAvailabilityStatus("ACTIVELY_LOOKING");
        c.setCreatedAt(OffsetDateTime.now());
        c.setUpdatedAt(OffsetDateTime.now());
        return c;
    }

    private Job job(long id, String status) {
        Job j = new Job();
        j.setId(id);
        Employer employer = new Employer();
        employer.setId(900L + id);
        employer.setCompanyName("Hope Works " + id);
        j.setEmployer(employer);
        j.setSlug("published-role-" + id);
        j.setTitle("Field Coordinator " + id);
        j.setDescription("Public description");
        j.setEmploymentType(Job.EMPLOYMENT_FULL_TIME);
        j.setWorkMode(Job.WORK_MODE_HYBRID);
        j.setSalaryCurrency("INR");
        j.setStatus(status);
        j.setApplicationDeadline(LocalDate.of(2026, 12, 31));
        j.setPublishedAt(Job.STATUS_PUBLISHED.equals(status)
                ? OffsetDateTime.parse("2026-09-01T10:15:30Z") : null);
        j.setCreatedAt(OffsetDateTime.now());
        j.setUpdatedAt(OffsetDateTime.now());
        return j;
    }

    private Resume resume(long id, long candidateId, boolean active) {
        Resume r = new Resume();
        r.setId(id);
        r.setCandidateId(candidateId);
        r.setFileUrl("legacy");
        r.setFileName("resume-" + id + ".pdf");
        r.setFileType("application/pdf");
        r.setFileSizeBytes(1000L);
        r.setActive(active);
        r.setCreatedAt(OffsetDateTime.now());
        return r;
    }

    private JobApplication application(long id, long jobId, long candidateId) {
        JobApplication app = new JobApplication();
        app.setId(id);
        Job j = job(jobId, Job.STATUS_PUBLISHED);
        j.setId(jobId);
        app.setJob(j);
        app.setCandidate(candidate(candidateId,
                dbUser(candidateId, "c" + candidateId + "@x.org")));
        app.setResumeId(null);
        app.setStatus(JobApplication.STATUS_SUBMITTED);
        app.setAppliedAt(OffsetDateTime.now());
        app.setCreatedAt(OffsetDateTime.now());
        app.setUpdatedAt(OffsetDateTime.now());
        return app;
    }

    private String tokenFor(long userId, String email) {
        return jwtTokenProvider.generateAccessToken(userId, email, List.of());
    }

    private String submitBody(long jobId, Long resumeId) {
        String resumePart = resumeId != null
                ? ", \"resumeId\":" + resumeId : "";
        return "{\"jobId\":" + jobId + resumePart
                + ", \"coverNote\":\"I am a strong fit.\"}";
    }

    // ------------------------------------------------------------------
    // authentication
    // ------------------------------------------------------------------

    @Test
    @DisplayName("anonymous submission is rejected 401 by the security chain")
    void anonymousSubmissionRejected() throws Exception {
        mockMvc.perform(post(ME_APPLICATIONS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submitBody(1L, null)))
                .andExpect(status().isUnauthorized());

        verify(applicationRepository, never()).save(any(JobApplication.class));
    }

    @Test
    @DisplayName("anonymous read endpoints are rejected 401")
    void anonymousReadsRejected() throws Exception {
        mockMvc.perform(get(ME_APPLICATIONS)).andExpect(status().isUnauthorized());
        mockMvc.perform(get(ME_APPLICATIONS + "/5")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("authenticated candidate submits an application (201, safe DTO, workflow side effects)")
    void authenticatedCandidateSubmits() throws Exception {
        User user = dbUser(2L, CANDIDATE_EMAIL, role("CANDIDATE"));
        when(userRepository.findByEmail(CANDIDATE_EMAIL)).thenReturn(Optional.of(user));
        when(candidateRepository.findByUserId(2L))
                .thenReturn(Optional.of(candidate(10L, user)));

        when(jobRepository.findById(1L)).thenReturn(Optional.of(job(1L, Job.STATUS_PUBLISHED)));
        when(applicationRepository.existsByJobIdAndCandidateId(1L, 10L)).thenReturn(false);

        when(applicationRepository.save(any(JobApplication.class)))
                .thenAnswer(inv -> {
                    JobApplication app = inv.getArgument(0);
                    app.setId(77L);
                    return app;
                });

        mockMvc.perform(post(ME_APPLICATIONS)
                        .header("Authorization", "Bearer " + tokenFor(2L, CANDIDATE_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submitBody(1L, null)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(77))
                .andExpect(jsonPath("$.data.job.id").value(1))
                .andExpect(jsonPath("$.data.job.title").value("Field Coordinator 1"))
                .andExpect(jsonPath("$.data.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.data.resumeId").doesNotExist())
                // candidate/employer internals never leak through the candidate DTO
                .andExpect(jsonPath("$.data.candidate").doesNotExist())
                .andExpect(jsonPath("$.data.employer").doesNotExist())
                .andExpect(jsonPath("$.data.employerNote").doesNotExist());

        // Workflow: initial history row (previousStatus NULL), audit row,
        // outbox event with the APPLICATION_STATUS_CHANGED vocabulary.
        verify(historyRepository).save(any(
                com.lokmit.foundation.employment.application.history.entity.ApplicationStatusHistory.class));
        verify(auditLogRepository).save(any(
                com.lokmit.foundation.audit.entity.AuditLog.class));
        org.mockito.ArgumentCaptor<com.lokmit.foundation.outbox.entity.OutboxEvent> outboxCaptor =
                org.mockito.ArgumentCaptor.forClass(
                        com.lokmit.foundation.outbox.entity.OutboxEvent.class);
        verify(outboxEventRepository).save(outboxCaptor.capture());
        var event = outboxCaptor.getValue();
        assertThat(event.getAggregateType()).isEqualTo("JOB_APPLICATION");
        assertThat(event.getAggregateId()).isEqualTo(77L);
        assertThat(event.getEventType())
                .isEqualTo("APPLICATION_STATUS_CHANGED");
        assertThat(event.getPayload()).contains("APPLICATION_STATUS_CHANGED");
        assertThat(event.getPayload()).contains("SUBMITTED");
        // No resume bytes, no secrets in the payload.
        assertThat(event.getPayload()).doesNotContain("resumeBytes");
        assertThat(event.getPayload()).doesNotContain("password");
    }

    @Test
    @DisplayName("candidateId is never accepted from the client (no such request field)")
    void candidateIdCannotBeSpoofed() throws Exception {
        User user = dbUser(2L, CANDIDATE_EMAIL, role("CANDIDATE"));
        when(userRepository.findByEmail(CANDIDATE_EMAIL)).thenReturn(Optional.of(user));
        when(candidateRepository.findByUserId(2L))
                .thenReturn(Optional.of(candidate(10L, user)));
        when(jobRepository.findById(1L)).thenReturn(Optional.of(job(1L, Job.STATUS_PUBLISHED)));
        when(applicationRepository.existsByJobIdAndCandidateId(1L, 10L)).thenReturn(false);
        when(applicationRepository.save(any(JobApplication.class)))
                .thenAnswer(inv -> {
                    JobApplication app = inv.getArgument(0);
                    app.setId(78L);
                    return app;
                });

        mockMvc.perform(post(ME_APPLICATIONS)
                        .header("Authorization", "Bearer " + tokenFor(2L, CANDIDATE_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"jobId\":1, \"candidateId\":9999}"))
                .andExpect(status().isCreated());

        // The persisted application belongs to candidate 10 (server-resolved),
        // never the spoofed 9999.
        org.mockito.ArgumentCaptor<JobApplication> appCaptor =
                org.mockito.ArgumentCaptor.forClass(JobApplication.class);
        verify(applicationRepository).save(appCaptor.capture());
        assertThat(appCaptor.getValue().getCandidate().getId()).isEqualTo(10L);
    }

    // ------------------------------------------------------------------
    // job visibility
    // ------------------------------------------------------------------

    @Test
    @DisplayName("DRAFT/CLOSED/ARCHIVED/unknown jobs all return the same masked 404")
    void nonPublishedJobIsMasked404() throws Exception {
        User user = dbUser(2L, CANDIDATE_EMAIL, role("CANDIDATE"));
        when(userRepository.findByEmail(CANDIDATE_EMAIL)).thenReturn(Optional.of(user));
        when(candidateRepository.findByUserId(2L))
                .thenReturn(Optional.of(candidate(10L, user)));
        for (long id : new long[]{11L, 12L, 13L}) {
            when(jobRepository.findById(id))
                    .thenReturn(Optional.of(job(id, id == 11 ? Job.STATUS_DRAFT
                            : id == 12 ? Job.STATUS_CLOSED : Job.STATUS_ARCHIVED)));
        }

        for (long id : new long[]{11L, 12L, 13L, 99L}) {
            mockMvc.perform(post(ME_APPLICATIONS)
                            .header("Authorization", "Bearer " + tokenFor(2L, CANDIDATE_EMAIL))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(submitBody(id, null)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false));
        }
        verify(applicationRepository, never()).save(any(JobApplication.class));
    }

    // ------------------------------------------------------------------
    // resume ownership
    // ------------------------------------------------------------------

    @Test
    @DisplayName("own ACTIVE resume attaches; foreign/inactive/nonexistent resume is masked 404")
    void resumeOwnershipEnforced() throws Exception {
        User user = dbUser(2L, CANDIDATE_EMAIL, role("CANDIDATE"));
        when(userRepository.findByEmail(CANDIDATE_EMAIL)).thenReturn(Optional.of(user));
        when(candidateRepository.findByUserId(2L))
                .thenReturn(Optional.of(candidate(10L, user)));
        when(jobRepository.findById(1L)).thenReturn(Optional.of(job(1L, Job.STATUS_PUBLISHED)));
        when(applicationRepository.existsByJobIdAndCandidateId(1L, 10L)).thenReturn(false);
        when(applicationRepository.save(any(JobApplication.class)))
                .thenAnswer(inv -> {
                    JobApplication app = inv.getArgument(0);
                    app.setId(79L);
                    return app;
                });
        when(resumeRepository.findByIdAndCandidateId(300L, 10L))
                .thenReturn(Optional.of(resume(300L, 10L, true)));

        // Own active resume → attaches.
        mockMvc.perform(post(ME_APPLICATIONS)
                        .header("Authorization", "Bearer " + tokenFor(2L, CANDIDATE_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submitBody(1L, 300L)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.resumeId").value(300));

        // Foreign resume (exists, other candidate) → masked 404.
        when(resumeRepository.findByIdAndCandidateId(400L, 10L)).thenReturn(Optional.empty());
        when(resumeRepository.findById(400L)).thenReturn(Optional.of(resume(400L, 99L, true)));
        mockMvc.perform(post(ME_APPLICATIONS)
                        .header("Authorization", "Bearer " + tokenFor(2L, CANDIDATE_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submitBody(1L, 400L)))
                .andExpect(status().isNotFound());

        // Inactive own resume → masked 404.
        when(resumeRepository.findByIdAndCandidateId(300L, 10L))
                .thenReturn(Optional.of(resume(300L, 10L, false)));
        mockMvc.perform(post(ME_APPLICATIONS)
                        .header("Authorization", "Bearer " + tokenFor(2L, CANDIDATE_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submitBody(1L, 300L)))
                .andExpect(status().isNotFound());

        // Nonexistent resume → same masked 404.
        when(resumeRepository.findByIdAndCandidateId(999L, 10L)).thenReturn(Optional.empty());
        when(resumeRepository.findById(999L)).thenReturn(Optional.empty());
        mockMvc.perform(post(ME_APPLICATIONS)
                        .header("Authorization", "Bearer " + tokenFor(2L, CANDIDATE_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submitBody(1L, 999L)))
                .andExpect(status().isNotFound());

        verify(resumeRepository, never()).save(any(Resume.class));
    }

    // ------------------------------------------------------------------
    // duplicates
    // ------------------------------------------------------------------

    @Test
    @DisplayName("duplicate application for the same job is rejected 409")
    void duplicateApplicationRejected() throws Exception {
        User user = dbUser(2L, CANDIDATE_EMAIL, role("CANDIDATE"));
        when(userRepository.findByEmail(CANDIDATE_EMAIL)).thenReturn(Optional.of(user));
        when(candidateRepository.findByUserId(2L))
                .thenReturn(Optional.of(candidate(10L, user)));
        when(jobRepository.findById(1L)).thenReturn(Optional.of(job(1L, Job.STATUS_PUBLISHED)));
        when(applicationRepository.existsByJobIdAndCandidateId(1L, 10L)).thenReturn(true);

        mockMvc.perform(post(ME_APPLICATIONS)
                        .header("Authorization", "Bearer " + tokenFor(2L, CANDIDATE_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submitBody(1L, null)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errors[0].code").value("CONFLICT"));

        verify(applicationRepository, never()).save(any(JobApplication.class));
    }

    // ------------------------------------------------------------------
    // validation
    // ------------------------------------------------------------------

    @Test
    @DisplayName("missing jobId or bad field values are rejected 400 before any business logic")
    void validationRejectsBadRequests() throws Exception {
        User user = dbUser(2L, CANDIDATE_EMAIL, role("CANDIDATE"));
        when(userRepository.findByEmail(CANDIDATE_EMAIL)).thenReturn(Optional.of(user));
        when(candidateRepository.findByUserId(2L))
                .thenReturn(Optional.of(candidate(10L, user)));

        mockMvc.perform(post(ME_APPLICATIONS)
                        .header("Authorization", "Bearer " + tokenFor(2L, CANDIDATE_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post(ME_APPLICATIONS)
                        .header("Authorization", "Bearer " + tokenFor(2L, CANDIDATE_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submitBody(0L, null)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post(ME_APPLICATIONS)
                        .header("Authorization", "Bearer " + tokenFor(2L, CANDIDATE_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submitBody(1L, -3L)))
                .andExpect(status().isBadRequest());

        verify(jobRepository, never()).findById(anyLong());
    }

    // ------------------------------------------------------------------
    // read APIs — ownership scoped
    // ------------------------------------------------------------------

    @Test
    @DisplayName("candidate sees only their own applications (list + single, 404-masking for foreign)")
    void readApisAreOwnershipScoped() throws Exception {
        User user = dbUser(2L, CANDIDATE_EMAIL, role("CANDIDATE"));
        when(userRepository.findByEmail(CANDIDATE_EMAIL)).thenReturn(Optional.of(user));
        when(candidateRepository.findByUserId(2L))
                .thenReturn(Optional.of(candidate(10L, user)));

        when(applicationRepository.findByCandidateIdOrderByCreatedAtDesc(
                eq(10L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(
                        List.of(application(7L, 1L, 10L)),
                        PageRequest.of(0, 20), 1));

        mockMvc.perform(get(ME_APPLICATIONS)
                        .header("Authorization", "Bearer " + tokenFor(2L, CANDIDATE_EMAIL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].id").value(7))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.items[0].candidate").doesNotExist());

        // Own application → 200. Foreign application (candidate 99) → same
        // 404 as a nonexistent one — no existence leak.
        when(applicationRepository.findByIdAndCandidateId(7L, 10L))
                .thenReturn(Optional.of(application(7L, 1L, 10L)));
        mockMvc.perform(get(ME_APPLICATIONS + "/7")
                        .header("Authorization", "Bearer " + tokenFor(2L, CANDIDATE_EMAIL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(7));

        when(applicationRepository.findByIdAndCandidateId(8L, 10L))
                .thenReturn(Optional.empty());
        when(applicationRepository.findById(8L))
                .thenReturn(Optional.of(application(8L, 1L, 99L)));
        mockMvc.perform(get(ME_APPLICATIONS + "/8")
                        .header("Authorization", "Bearer " + tokenFor(2L, CANDIDATE_EMAIL)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errors[0].code").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("a user without a candidate profile (admin/employer) receives a plain 404")
    void userWithoutCandidateProfileGets404() throws Exception {
        User user = dbUser(2L, CANDIDATE_EMAIL, role("CANDIDATE"));
        when(userRepository.findByEmail(CANDIDATE_EMAIL)).thenReturn(Optional.of(user));
        when(candidateRepository.findByUserId(2L)).thenReturn(Optional.empty());

        mockMvc.perform(post(ME_APPLICATIONS)
                        .header("Authorization", "Bearer " + tokenFor(2L, CANDIDATE_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submitBody(1L, null)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("oversized page request is rejected 400 (PageParams cap)")
    void oversizedPageRejected() throws Exception {
        User user = dbUser(2L, CANDIDATE_EMAIL, role("CANDIDATE"));
        when(userRepository.findByEmail(CANDIDATE_EMAIL)).thenReturn(Optional.of(user));
        when(candidateRepository.findByUserId(2L))
                .thenReturn(Optional.of(candidate(10L, user)));

        mockMvc.perform(get(ME_APPLICATIONS).param("size", "101")
                        .header("Authorization", "Bearer " + tokenFor(2L, CANDIDATE_EMAIL)))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------
    // security regression — A7.3 admin application APIs unchanged
    // ------------------------------------------------------------------

    @Test
    @DisplayName("admin application APIs remain permission-guarded: anonymous 401, candidate 403")
    void adminApplicationApisRemainProtected() throws Exception {
        mockMvc.perform(get(ADMIN_APPLICATIONS))
                .andExpect(status().isUnauthorized());

        User candidateUser = dbUser(2L, CANDIDATE_EMAIL, role("CANDIDATE"));
        when(userRepository.findByEmail(CANDIDATE_EMAIL))
                .thenReturn(Optional.of(candidateUser));
        mockMvc.perform(get(ADMIN_APPLICATIONS)
                        .header("Authorization", "Bearer " + tokenFor(2L, CANDIDATE_EMAIL)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post(ME_APPLICATIONS).contentType(MediaType.APPLICATION_JSON)
                        .content(submitBody(1L, null)))
                .andExpect(status().isUnauthorized());

        // Admin lifecycle transitions stay anonymous-401 exactly as before.
        mockMvc.perform(post(ADMIN_APPLICATIONS + "/5/start-review"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post(ADMIN_APPLICATIONS + "/5/decide")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete(ADMIN_APPLICATIONS + "/5"))
                .andExpect(status().isUnauthorized());

        verify(applicationRepository, never()).save(any(JobApplication.class));
        verify(historyRepository, never()).save(any(
                com.lokmit.foundation.employment.application.history.entity.ApplicationStatusHistory.class));
    }
}
