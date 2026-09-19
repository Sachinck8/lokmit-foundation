package com.lokmit.foundation.employment.controller;

import com.lokmit.foundation.audit.repository.AuditLogRepository;
import com.lokmit.foundation.common.constants.ApiPaths;
import com.lokmit.foundation.employment.application.history.controller.ApplicationHistoryController;
import com.lokmit.foundation.employment.application.controller.CandidateApplicationController;
import com.lokmit.foundation.employment.application.entity.JobApplication;
import com.lokmit.foundation.employment.application.history.entity.ApplicationStatusHistory;
import com.lokmit.foundation.employment.application.history.repository.ApplicationStatusHistoryRepository;
import com.lokmit.foundation.employment.application.history.service.ApplicationStatusHistoryService;
import com.lokmit.foundation.employment.application.repository.JobApplicationRepository;
import com.lokmit.foundation.employment.application.service.ApplicationService;
import com.lokmit.foundation.employment.application.service.CandidateApplicationService;
import com.lokmit.foundation.employment.candidate.entity.Candidate;
import com.lokmit.foundation.employment.candidate.repository.CandidateRepository;
import com.lokmit.foundation.employment.employer.entity.Employer;
import com.lokmit.foundation.employment.job.entity.Job;
import com.lokmit.foundation.employment.job.repository.JobRepository;
import com.lokmit.foundation.employment.resume.repository.ResumeRepository;
import com.lokmit.foundation.employment.resume.service.ResumeOwnershipService;
import com.lokmit.foundation.notification.repository.NotificationRepository;
import com.lokmit.foundation.outbox.config.OutboxRelayConfig;
import com.lokmit.foundation.outbox.repository.OutboxEventRepository;
import com.lokmit.foundation.outbox.service.OutboxService;
import com.lokmit.foundation.audit.service.AuditLogService;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A12 security and behavior matrix through the REAL Spring Security filter
 * chain (same setup as the A8/A9/A10/A11 matrices).
 *
 * <p>Coverage: candidate status-history read (ownership gate BEFORE the
 * history query — foreign/unknown ids produce the identical masked 404;
 * existing A7.4 repository read reused with changedAt-DESC ordering and
 * PageParams pagination; candidate-safe DTO proves no changedBy/row-id/note
 * leak) plus the admin history endpoint regression (unchanged permission
 * guard).</p>
 *
 * <p>The dashboard status breakdown deliberately reuses the existing
 * paginated {@code GET /candidates/me/applications} endpoint (A9) — no new
 * aggregate endpoint exists, so its security properties (server-side
 * candidate resolution, no candidateId parameter, ownership-scoped pages)
 * are already covered by {@link CandidateApplicationsSecurityTest}.</p>
 */
@WebMvcTest(controllers = {CandidateApplicationController.class,
        ApplicationHistoryController.class})
@Import({SecurityConfig.class, CorsConfig.class, JwtKeyConfig.class,
        JwtTokenProvider.class, CustomUserDetailsService.class, SecurityUtils.class,
        CandidateApplicationService.class, ApplicationService.class,
        ApplicationStatusHistoryService.class, AuditLogService.class,
        ResumeOwnershipService.class, OutboxService.class, OutboxRelayConfig.class})
@AutoConfigureMockMvc
class CandidateApplicationHistorySecurityTest {

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

    private User dbUser(long id, String email) {
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
        Role role = new Role();
        role.setCode("CANDIDATE");
        Permission p = new Permission();
        p.setCode("PROFILE_SELF");
        role.setPermissions(Set.of(p));
        user.setRoles(Set.of(role));
        return user;
    }

    private Candidate candidate(long id, User user) {
        Candidate c = new Candidate();
        c.setId(id);
        c.setUser(user);
        c.setPhone("+91-900000000" + id);
        c.setCurrentLocation("Patna");
        c.setAvailabilityStatus("ACTIVELY_LOOKING");
        c.setCreatedAt(OffsetDateTime.now());
        c.setUpdatedAt(OffsetDateTime.now());
        return c;
    }

    private JobApplication application(long id, long candidateId, String status) {
        JobApplication app = new JobApplication();
        app.setId(id);
        Job j = new Job();
        j.setId(1L);
        Employer employer = new Employer();
        employer.setId(900L);
        employer.setCompanyName("Hope Works");
        j.setEmployer(employer);
        j.setSlug("published-role");
        j.setTitle("Field Coordinator");
        j.setStatus(Job.STATUS_PUBLISHED);
        app.setJob(j);
        app.setCandidate(candidate(candidateId, dbUser(candidateId, "c" + candidateId + "@x.org")));
        app.setResumeId(null);
        app.setStatus(status);
        app.setAppliedAt(OffsetDateTime.now());
        app.setCreatedAt(OffsetDateTime.now());
        app.setUpdatedAt(OffsetDateTime.now());
        return app;
    }

    private ApplicationStatusHistory historyRow(String previous, String next,
                                                OffsetDateTime changedAt) {
        ApplicationStatusHistory h = new ApplicationStatusHistory();
        h.setId(1L);
        h.setApplication(application(7L, 10L, next));
        h.setPreviousStatus(previous);
        h.setNewStatus(next);
        h.setChangedBy(501L);
        h.setChangedAt(changedAt);
        h.setNote("internal admin note");
        return h;
    }

    private String tokenFor(long userId, String email) {
        return jwtTokenProvider.generateAccessToken(userId, email, List.of());
    }

    private void stubAuthenticatedCandidate(long userId, long candidateId) {
        User user = dbUser(userId, CANDIDATE_EMAIL);
        when(userRepository.findByEmail(CANDIDATE_EMAIL)).thenReturn(Optional.of(user));
        when(candidateRepository.findByUserId(userId))
                .thenReturn(Optional.of(candidate(candidateId, user)));
    }

    // ------------------------------------------------------------------
    // authentication
    // ------------------------------------------------------------------

    @Test
    @DisplayName("anonymous history read is rejected 401 by the security chain")
    void anonymousHistoryRejected() throws Exception {
        mockMvc.perform(get(ME_APPLICATIONS + "/7/history"))
                .andExpect(status().isUnauthorized());
        verify(historyRepository, never()).findByApplicationIdOrderByChangedAtDesc(
                any(Long.class), any(Pageable.class));
    }

    // ------------------------------------------------------------------
    // own history read
    // ------------------------------------------------------------------

    @Test
    @DisplayName("candidate reads own application history (safe DTO: no changedBy, no row id, no note)")
    void candidateReadsOwnHistory() throws Exception {
        stubAuthenticatedCandidate(2L, 10L);
        when(applicationRepository.findByIdAndCandidateId(7L, 10L))
                .thenReturn(Optional.of(application(7L, 10L, JobApplication.STATUS_SHORTLISTED)));
        when(historyRepository.findByApplicationIdOrderByChangedAtDesc(
                eq(7L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(
                        List.of(
                                historyRow(JobApplication.STATUS_UNDER_REVIEW,
                                        JobApplication.STATUS_SHORTLISTED,
                                        OffsetDateTime.parse("2026-09-10T12:00:00Z")),
                                historyRow(null, JobApplication.STATUS_SUBMITTED,
                                        OffsetDateTime.parse("2026-09-01T09:00:00Z"))),
                        PageRequest.of(0, 20), 2));

        mockMvc.perform(get(ME_APPLICATIONS + "/7/history")
                        .header("Authorization", "Bearer " + tokenFor(2L, CANDIDATE_EMAIL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.totalItems").value(2))
                .andExpect(jsonPath("$.data.items[0].previousStatus").value("UNDER_REVIEW"))
                .andExpect(jsonPath("$.data.items[0].newStatus").value("SHORTLISTED"))
                .andExpect(jsonPath("$.data.items[0].changedAt").exists())
                // Candidate-safe DTO: internal fields never serialize.
                .andExpect(jsonPath("$.data.items[0].changedBy").doesNotExist())
                .andExpect(jsonPath("$.data.items[0].id").doesNotExist())
                .andExpect(jsonPath("$.data.items[0].note").doesNotExist())
                .andExpect(jsonPath("$.data.items[0].applicationId").doesNotExist());

        // The read is scoped to the owned application, newest first.
        verify(historyRepository).findByApplicationIdOrderByChangedAtDesc(
                eq(7L), any(Pageable.class));
    }

    @Test
    @DisplayName("pagination parameters reach the existing repository read (PageParams)")
    void paginationUsesExistingConventions() throws Exception {
        stubAuthenticatedCandidate(2L, 10L);
        when(applicationRepository.findByIdAndCandidateId(7L, 10L))
                .thenReturn(Optional.of(application(7L, 10L, JobApplication.STATUS_SUBMITTED)));
        when(historyRepository.findByApplicationIdOrderByChangedAtDesc(
                eq(7L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(1, 10), 0));

        mockMvc.perform(get(ME_APPLICATIONS + "/7/history")
                        .param("page", "1").param("size", "10")
                        .header("Authorization", "Bearer " + tokenFor(2L, CANDIDATE_EMAIL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(10))
                .andExpect(jsonPath("$.data.items").isArray());

        org.mockito.ArgumentCaptor<Pageable> captor =
                org.mockito.ArgumentCaptor.forClass(Pageable.class);
        verify(historyRepository).findByApplicationIdOrderByChangedAtDesc(eq(7L), captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().getPageNumber()).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(captor.getValue().getPageSize()).isEqualTo(10);
    }

    @Test
    @DisplayName("oversized page request is rejected 400 (PageParams cap)")
    void oversizedPageRejected() throws Exception {
        stubAuthenticatedCandidate(2L, 10L);
        mockMvc.perform(get(ME_APPLICATIONS + "/7/history").param("size", "101")
                        .header("Authorization", "Bearer " + tokenFor(2L, CANDIDATE_EMAIL)))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------
    // ownership masking
    // ------------------------------------------------------------------

    @Test
    @DisplayName("foreign and unknown application ids are the same masked 404 (no existence leak)")
    void foreignAndUnknownHistoryMasked() throws Exception {
        stubAuthenticatedCandidate(2L, 10L);

        // Foreign application (candidate 99's id 8) — exists, but not ours.
        when(applicationRepository.findByIdAndCandidateId(8L, 10L))
                .thenReturn(Optional.empty());
        when(applicationRepository.findById(8L))
                .thenReturn(Optional.of(application(8L, 99L, JobApplication.STATUS_SUBMITTED)));

        // Unknown application.
        when(applicationRepository.findByIdAndCandidateId(77L, 10L))
                .thenReturn(Optional.empty());
        when(applicationRepository.findById(77L)).thenReturn(Optional.empty());

        for (long id : new long[]{8L, 77L}) {
            mockMvc.perform(get(ME_APPLICATIONS + "/" + id + "/history")
                            .header("Authorization", "Bearer " + tokenFor(2L, CANDIDATE_EMAIL)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("NOT_FOUND"));
        }

        // Ownership gate fired BEFORE the history query — nothing was read.
        verify(historyRepository, never()).findByApplicationIdOrderByChangedAtDesc(
                any(Long.class), any(Pageable.class));
    }

    // ------------------------------------------------------------------
    // admin regression — A7.4 admin history endpoint unchanged
    // ------------------------------------------------------------------

    @Test
    @DisplayName("admin history endpoint remains permission-guarded: anonymous 401, candidate 403")
    void adminHistoryRemainsProtected() throws Exception {
        mockMvc.perform(get(ADMIN_APPLICATIONS + "/7/history"))
                .andExpect(status().isUnauthorized());

        stubAuthenticatedCandidate(2L, 10L);
        mockMvc.perform(get(ADMIN_APPLICATIONS + "/7/history")
                        .header("Authorization", "Bearer " + tokenFor(2L, CANDIDATE_EMAIL)))
                .andExpect(status().isForbidden());

        // The candidate endpoint never touched the admin path's data.
        verify(historyRepository, never()).findByApplicationIdOrderByChangedAtDesc(
                eq(7L), any(Pageable.class));
    }
}
