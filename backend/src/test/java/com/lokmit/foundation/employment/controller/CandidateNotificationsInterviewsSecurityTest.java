package com.lokmit.foundation.employment.controller;

import com.lokmit.foundation.audit.repository.AuditLogRepository;
import com.lokmit.foundation.audit.service.AuditLogService;
import com.lokmit.foundation.common.constants.ApiPaths;
import com.lokmit.foundation.employment.application.controller.ApplicationController;
import com.lokmit.foundation.employment.application.entity.JobApplication;
import com.lokmit.foundation.employment.application.history.repository.ApplicationStatusHistoryRepository;
import com.lokmit.foundation.employment.application.history.service.ApplicationStatusHistoryService;
import com.lokmit.foundation.employment.application.interview.controller.CandidateInterviewController;
import com.lokmit.foundation.employment.application.interview.controller.InterviewController;
import com.lokmit.foundation.employment.application.interview.entity.Interview;
import com.lokmit.foundation.employment.application.interview.repository.InterviewRepository;
import com.lokmit.foundation.employment.application.interview.service.CandidateInterviewService;
import com.lokmit.foundation.employment.application.interview.service.InterviewService;
import com.lokmit.foundation.employment.application.repository.JobApplicationRepository;
import com.lokmit.foundation.employment.application.service.ApplicationService;
import com.lokmit.foundation.employment.candidate.entity.Candidate;
import com.lokmit.foundation.employment.candidate.repository.CandidateRepository;
import com.lokmit.foundation.employment.employer.entity.Employer;
import com.lokmit.foundation.employment.job.entity.Job;
import com.lokmit.foundation.employment.job.repository.JobRepository;
import com.lokmit.foundation.employment.resume.repository.ResumeRepository;
import com.lokmit.foundation.employment.resume.service.ResumeOwnershipService;
import com.lokmit.foundation.notification.controller.CandidateNotificationController;
import com.lokmit.foundation.notification.controller.NotificationController;
import com.lokmit.foundation.notification.entity.Notification;
import com.lokmit.foundation.notification.repository.NotificationRepository;
import com.lokmit.foundation.notification.service.NotificationService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A14 security matrix through the REAL Spring Security filter chain.
 * Notifications: candidate routes over the existing user-scoped A7.5
 * service (anonymous 401; own list/unread-count/detail + markRead;
 * foreign/unknown masked 404; admin API remains notifications:manage-guarded).
 * Interviews: read-only candidate visibility through the
 * application-candidate ownership chain (own list/detail; foreign/unknown
 * masked 404; admin A7.4 CRUD remains permission-guarded).
 */
@WebMvcTest(controllers = {CandidateNotificationController.class,
        NotificationController.class,
        CandidateInterviewController.class, InterviewController.class,
        ApplicationController.class})
@Import({SecurityConfig.class, CorsConfig.class, JwtKeyConfig.class,
        JwtTokenProvider.class, CustomUserDetailsService.class, SecurityUtils.class,
        NotificationService.class, CandidateInterviewService.class,
        InterviewService.class, ApplicationService.class,
        ApplicationStatusHistoryService.class, AuditLogService.class,
        ResumeOwnershipService.class, OutboxService.class, OutboxRelayConfig.class})
@AutoConfigureMockMvc
class CandidateNotificationsInterviewsSecurityTest {

    private static final String ME_NOTIFICATIONS = ApiPaths.CANDIDATE_ME_NOTIFICATIONS;
    private static final String ME_INTERVIEWS = ApiPaths.CANDIDATE_ME_INTERVIEWS;
    private static final String ADMIN_NOTIFICATIONS = ApiPaths.ADMIN_NOTIFICATIONS;
    private static final String ADMIN_INTERVIEWS = ApiPaths.ADMIN_APPLICATION_INTERVIEWS;

    private static final String CANDIDATE_EMAIL = "candidate@lokmitfoundation.org";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationRepository notificationRepository;

    @MockitoBean
    private InterviewRepository interviewRepository;

    @MockitoBean
    private JobApplicationRepository applicationRepository;

    @MockitoBean
    private JobRepository jobRepository;

    @MockitoBean
    private ResumeRepository resumeRepository;

    @MockitoBean
    private CandidateRepository candidateRepository;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private ApplicationStatusHistoryRepository historyRepository;

    @MockitoBean
    private AuditLogRepository auditLogRepository;

    @MockitoBean
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void resetStubs() {
        reset(notificationRepository, interviewRepository, applicationRepository,
                jobRepository, resumeRepository, candidateRepository,
                userRepository, historyRepository, auditLogRepository,
                outboxEventRepository);
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
        app.setStatus(status);
        app.setAppliedAt(OffsetDateTime.now());
        app.setCreatedAt(OffsetDateTime.now());
        app.setUpdatedAt(OffsetDateTime.now());
        return app;
    }

    private Interview interview(long id, long applicationId, String status) {
        return interview(id, applicationId, 10L, status);
    }

    private Interview interview(long id, long applicationId, long candidateId, String status) {
        Interview i = new Interview();
        i.setId(id);
        i.setApplication(application(applicationId, candidateId, JobApplication.STATUS_SHORTLISTED));
        i.setScheduledAt(OffsetDateTime.parse("2026-10-01T10:30+05:30"));
        i.setMode(Interview.MODE_REMOTE);
        i.setLocation("Video call link shared by email");
        i.setStatus(status);
        i.setNotes("Join 5 minutes early.");
        i.setCreatedAt(OffsetDateTime.now());
        i.setUpdatedAt(OffsetDateTime.now());
        return i;
    }

    private Notification notification(long id, long recipientUserId, boolean read) {
        Notification n = new Notification();
        n.setId(id);
        n.setRecipientUserId(recipientUserId);
        n.setType(Notification.TYPE_APPLICATION_STATUS_CHANGED);
        n.setTitle("Application status updated: SHORTLISTED");
        n.setBody("Your application moved from SUBMITTED to SHORTLISTED.");
        n.setEntityType("JOB_APPLICATION");
        n.setEntityId(7L);
        n.setReadAt(read ? OffsetDateTime.now() : null);
        n.setCreatedAt(OffsetDateTime.now());
        return n;
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
    // notifications
    // ------------------------------------------------------------------

    @Test
    @DisplayName("anonymous notification access is rejected 401")
    void anonymousNotificationsRejected() throws Exception {
        mockMvc.perform(get(ME_NOTIFICATIONS)).andExpect(status().isUnauthorized());
        mockMvc.perform(get(ME_NOTIFICATIONS + "/unread-count"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get(ME_NOTIFICATIONS + "/5"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(patch(ME_NOTIFICATIONS + "/5/read"))
                .andExpect(status().isUnauthorized());
        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    @DisplayName("candidate lists own notifications (no recipient id leaks)")
    void candidateListsOwnNotifications() throws Exception {
        stubAuthenticatedCandidate(2L, 10L);
        when(notificationRepository.findAll(
                any(org.springframework.data.jpa.domain.Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(
                        List.of(notification(1L, 2L, false), notification(2L, 2L, true)),
                        PageRequest.of(0, 20), 2));

        mockMvc.perform(get(ME_NOTIFICATIONS)
                        .header("Authorization", "Bearer " + tokenFor(2L, CANDIDATE_EMAIL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].title").exists())
                .andExpect(jsonPath("$.data.items[0].recipientUserId").doesNotExist())
                .andExpect(jsonPath("$.data.items[0].recipient").doesNotExist());
    }

    @Test
    @DisplayName("unread count is scoped to the authenticated user")
    void candidateUnreadCount() throws Exception {
        stubAuthenticatedCandidate(2L, 10L);
        when(notificationRepository.countByRecipientUserIdAndReadAtIsNull(2L)).thenReturn(3L);

        mockMvc.perform(get(ME_NOTIFICATIONS + "/unread-count")
                        .header("Authorization", "Bearer " + tokenFor(2L, CANDIDATE_EMAIL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount").value(3));
    }

    @Test
    @DisplayName("mark-read works on own notification; foreign/unknown masked 404")
    void markReadOwnership() throws Exception {
        stubAuthenticatedCandidate(2L, 10L);
        Notification own = notification(5L, 2L, false);
        when(notificationRepository.findByIdAndRecipientUserId(5L, 2L))
                .thenReturn(Optional.of(own));
        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(patch(ME_NOTIFICATIONS + "/5/read")
                        .header("Authorization", "Bearer " + tokenFor(2L, CANDIDATE_EMAIL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.readAt").exists());

        // Foreign notification (user 99's) — same masked 404 as unknown.
        when(notificationRepository.findByIdAndRecipientUserId(6L, 2L))
                .thenReturn(Optional.empty());
        when(notificationRepository.findById(6L))
                .thenReturn(Optional.of(notification(6L, 99L, false)));
        mockMvc.perform(patch(ME_NOTIFICATIONS + "/6/read")
                        .header("Authorization", "Bearer " + tokenFor(2L, CANDIDATE_EMAIL)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));

        // Only the own notification was persisted.
        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    // ------------------------------------------------------------------
    // interviews
    // ------------------------------------------------------------------

    @Test
    @DisplayName("candidate lists interviews for own application")
    void candidateListsOwnInterviews() throws Exception {
        stubAuthenticatedCandidate(2L, 10L);
        when(applicationRepository.findById(7L))
                .thenReturn(Optional.of(application(7L, 10L, JobApplication.STATUS_SHORTLISTED)));
        when(interviewRepository.findByApplicationIdOrderByScheduledAtDesc(
                eq(7L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(
                        List.of(interview(3L, 7L, Interview.STATUS_SCHEDULED)),
                        PageRequest.of(0, 20), 1));

        mockMvc.perform(get(ME_INTERVIEWS).param("applicationId", "7")
                        .header("Authorization", "Bearer " + tokenFor(2L, CANDIDATE_EMAIL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].mode").value("REMOTE"))
                .andExpect(jsonPath("$.data.items[0].status").value("SCHEDULED"))
                .andExpect(jsonPath("$.data.items[0].scheduledAt").value("2026-10-01T10:30:00+05:30"))
                .andExpect(jsonPath("$.data.items[0].updatedAt").doesNotExist())
                .andExpect(jsonPath("$.data.items[0].candidate").doesNotExist());
    }

    @Test
    @DisplayName("foreign or unknown application interviews are masked 404")
    void foreignApplicationInterviewsMasked() throws Exception {
        stubAuthenticatedCandidate(2L, 10L);
        // Candidate 99 owns application 8.
        when(applicationRepository.findById(8L))
                .thenReturn(Optional.of(application(8L, 99L, JobApplication.STATUS_SHORTLISTED)));

        mockMvc.perform(get(ME_INTERVIEWS).param("applicationId", "8")
                        .header("Authorization", "Bearer " + tokenFor(2L, CANDIDATE_EMAIL)))
                .andExpect(status().isNotFound());

        // Unknown application — identical 404.
        when(applicationRepository.findById(77L)).thenReturn(Optional.empty());
        mockMvc.perform(get(ME_INTERVIEWS).param("applicationId", "77")
                        .header("Authorization", "Bearer " + tokenFor(2L, CANDIDATE_EMAIL)))
                .andExpect(status().isNotFound());

        // The interview repository is never reached through a foreign application.
        verify(interviewRepository, never()).findByApplicationIdOrderByScheduledAtDesc(
                eq(8L), any(Pageable.class));
    }

    @Test
    @DisplayName("candidate views own interview by id; foreign/unknown masked 404")
    void candidateInterviewDetailOwnership() throws Exception {
        stubAuthenticatedCandidate(2L, 10L);
        when(interviewRepository.findById(3L))
                .thenReturn(Optional.of(interview(3L, 7L, Interview.STATUS_SCHEDULED)));

        mockMvc.perform(get(ME_INTERVIEWS + "/3")
                        .header("Authorization", "Bearer " + tokenFor(2L, CANDIDATE_EMAIL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(3))
                .andExpect(jsonPath("$.data.status").value("SCHEDULED"))
                .andExpect(jsonPath("$.data.scheduledAt").value("2026-10-01T10:30:00+05:30"))
                .andExpect(jsonPath("$.data.updatedAt").doesNotExist());

        // Foreign interview (on candidate 99's application 8) — masked 404.
        when(interviewRepository.findById(4L))
                .thenReturn(Optional.of(interview(4L, 8L, 99L, Interview.STATUS_SCHEDULED)));
        when(applicationRepository.findById(8L))
                .thenReturn(Optional.of(application(8L, 99L, JobApplication.STATUS_SHORTLISTED)));
        mockMvc.perform(get(ME_INTERVIEWS + "/4")
                        .header("Authorization", "Bearer " + tokenFor(2L, CANDIDATE_EMAIL)))
                .andExpect(status().isNotFound());

        // Unknown interview — identical 404.
        when(interviewRepository.findById(77L)).thenReturn(Optional.empty());
        mockMvc.perform(get(ME_INTERVIEWS + "/77")
                        .header("Authorization", "Bearer " + tokenFor(2L, CANDIDATE_EMAIL)))
                .andExpect(status().isNotFound());

        verify(interviewRepository, never()).delete(any(Interview.class));
        verify(interviewRepository, never()).save(any(Interview.class));
    }

    // ------------------------------------------------------------------
    // admin regression
    // ------------------------------------------------------------------

    @Test
    @DisplayName("admin notification + interview APIs remain permission-guarded")
    void adminApisRemainProtected() throws Exception {
        mockMvc.perform(get(ADMIN_NOTIFICATIONS)).andExpect(status().isUnauthorized());
        mockMvc.perform(get(ADMIN_INTERVIEWS, 7L)).andExpect(status().isUnauthorized());

        stubAuthenticatedCandidate(2L, 10L);
        mockMvc.perform(get(ADMIN_NOTIFICATIONS)
                        .header("Authorization", "Bearer " + tokenFor(2L, CANDIDATE_EMAIL)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(ADMIN_INTERVIEWS, 7L)
                        .header("Authorization", "Bearer " + tokenFor(2L, CANDIDATE_EMAIL)))
                .andExpect(status().isForbidden());
    }
}
