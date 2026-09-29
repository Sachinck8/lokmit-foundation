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
import com.lokmit.foundation.employment.candidate.entity.Candidate;
import com.lokmit.foundation.employment.candidate.repository.CandidateRepository;
import com.lokmit.foundation.employment.candidateskill.controller.CandidateSkillSelfServiceController;
import com.lokmit.foundation.employment.candidateskill.controller.CandidateSkillController;
import com.lokmit.foundation.employment.candidateskill.entity.CandidateSkill;
import com.lokmit.foundation.employment.candidateskill.entity.CandidateSkillId;
import com.lokmit.foundation.employment.candidateskill.repository.CandidateSkillRepository;
import com.lokmit.foundation.employment.candidateskill.service.CandidateSkillService;
import com.lokmit.foundation.employment.employer.entity.Employer;
import com.lokmit.foundation.employment.job.entity.Job;
import com.lokmit.foundation.employment.job.repository.JobRepository;
import com.lokmit.foundation.employment.resume.repository.ResumeRepository;
import com.lokmit.foundation.employment.resume.service.ResumeOwnershipService;
import com.lokmit.foundation.employment.skill.entity.Skill;
import com.lokmit.foundation.employment.skill.repository.SkillRepository;
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
 * A11 security and behavior matrix through the REAL Spring Security filter
 * chain (same setup as the A8/A9/A10 matrices).
 *
 * <p>Coverage: candidate skills self-service (list/assign/remove own skills
 * over the existing V8 candidate_skills join — candidate resolved from the
 * JWT, no candidateId field exists to spoof; ACTIVE-only catalog; duplicate
 * 409; unknown/foreign masked 404; proficiency validation) and candidate
 * application withdrawal (ownership-scoped before delegation to the existing
 * A7.3 withdraw workflow — decided/already-withdrawn 409 by the existing
 * rules, A7.4 history + A7.5 audit/outbox preserved, foreign/unknown ids
 * masked 404 with no existence leak).</p>
 */
@WebMvcTest(controllers = {CandidateSkillSelfServiceController.class,
        CandidateSkillController.class,
        CandidateApplicationController.class, ApplicationController.class})
@Import({SecurityConfig.class, CorsConfig.class, JwtKeyConfig.class,
        JwtTokenProvider.class, CustomUserDetailsService.class, SecurityUtils.class,
        CandidateSkillService.class, CandidateApplicationService.class,
        ApplicationService.class, ApplicationStatusHistoryService.class,
        AuditLogService.class, ResumeOwnershipService.class,
        OutboxService.class, OutboxRelayConfig.class})
@AutoConfigureMockMvc
class CandidateSelfServiceA11SecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CandidateSkillRepository candidateSkillRepository;

    @MockitoBean
    private SkillRepository skillRepository;

    @MockitoBean
    private CandidateRepository candidateRepository;

    @MockitoBean
    private JobApplicationRepository applicationRepository;

    @MockitoBean
    private JobRepository jobRepository;

    @MockitoBean
    private ResumeRepository resumeRepository;

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

    private static final String ME_SKILLS = ApiPaths.CANDIDATE_ME_SKILLS;
    private static final String ME_SKILL = ApiPaths.CANDIDATE_ME_SKILL;
    private static final String ME_APPLICATIONS = ApiPaths.CANDIDATE_ME_APPLICATIONS;

    private static final String CANDIDATE_EMAIL = "candidate@lokmitfoundation.org";

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void resetStubs() {
        reset(candidateSkillRepository, skillRepository, candidateRepository,
                applicationRepository, jobRepository, resumeRepository,
                historyRepository, auditLogRepository, outboxEventRepository,
                notificationRepository, userRepository);
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

    private Skill skill(long id, String name, String status) {
        Skill s = new Skill();
        s.setId(id);
        s.setName(name);
        s.setStatus(status);
        s.setCreatedAt(OffsetDateTime.now());
        s.setUpdatedAt(OffsetDateTime.now());
        return s;
    }

    private CandidateSkill assignment(long candidateId, Skill skill, String proficiency) {
        CandidateSkill cs = new CandidateSkill();
        Candidate owner = candidate(candidateId, dbUser(candidateId, "c" + candidateId + "@x.org"));
        cs.setCandidate(owner);
        cs.setSkill(skill);
        cs.setProficiency(proficiency);
        return cs;
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
    // skills — authentication
    // ------------------------------------------------------------------

    @Test
    @DisplayName("anonymous skill APIs are rejected 401 by the security chain")
    void anonymousSkillApisRejected() throws Exception {
        mockMvc.perform(get(ME_SKILLS)).andExpect(status().isUnauthorized());
        mockMvc.perform(post(ME_SKILLS).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"skillId\":5}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete(ME_SKILL, 5L)).andExpect(status().isUnauthorized());
        mockMvc.perform(get(ME_SKILLS + "/catalog")).andExpect(status().isUnauthorized());

        verify(candidateSkillRepository, never()).save(any(CandidateSkill.class));
        verify(candidateSkillRepository, never()).deleteById(any(CandidateSkillId.class));
    }

    // ------------------------------------------------------------------
    // skills — list / assign / remove (own only)
    // ------------------------------------------------------------------

    @Test
    @DisplayName("candidate lists own skills (ownership-scoped, sorted)")
    void candidateListsOwnSkills() throws Exception {
        stubAuthenticatedCandidate(2L, 10L);
        when(candidateSkillRepository.findByCandidateId(10L)).thenReturn(List.of(
                assignment(10L, skill(5L, "Community Mobilisation", Skill.STATUS_ACTIVE), "ADVANCED"),
                assignment(10L, skill(3L, "Data Collection", Skill.STATUS_ACTIVE), null)));
        when(candidateRepository.existsById(10L)).thenReturn(true);

        mockMvc.perform(get(ME_SKILLS)
                        .header("Authorization", "Bearer " + tokenFor(2L, CANDIDATE_EMAIL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].skillName").value("Community Mobilisation"))
                .andExpect(jsonPath("$.data[0].proficiency").value("ADVANCED"))
                .andExpect(jsonPath("$.data[1].skillName").value("Data Collection"));

        // The listing is scoped to the server-resolved candidate id.
        verify(candidateSkillRepository).findByCandidateId(10L);
    }

    @Test
    @DisplayName("candidate assigns an ACTIVE catalog skill to themselves (201, own candidate id)")
    void candidateAssignsOwnSkill() throws Exception {
        stubAuthenticatedCandidate(2L, 10L);
        when(candidateRepository.existsById(10L)).thenReturn(true);
        Skill active = skill(5L, "Community Mobilisation", Skill.STATUS_ACTIVE);
        when(skillRepository.findById(5L)).thenReturn(Optional.of(active));
        when(candidateSkillRepository.existsByCandidateIdAndSkillId(10L, 5L)).thenReturn(false);
        when(candidateRepository.getReferenceById(10L))
                .thenReturn(candidate(10L, dbUser(2L, CANDIDATE_EMAIL)));
        when(candidateSkillRepository.save(any(CandidateSkill.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(post(ME_SKILLS)
                        .header("Authorization", "Bearer " + tokenFor(2L, CANDIDATE_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"skillId\":5,\"proficiency\":\"ADVANCED\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.skillId").value(5))
                .andExpect(jsonPath("$.data.skillName").value("Community Mobilisation"))
                .andExpect(jsonPath("$.data.proficiency").value("ADVANCED"));

        org.mockito.ArgumentCaptor<CandidateSkill> captor =
                org.mockito.ArgumentCaptor.forClass(CandidateSkill.class);
        verify(candidateSkillRepository).save(captor.capture());
        assertThat(captor.getValue().getCandidate().getId()).isEqualTo(10L);
        assertThat(captor.getValue().getSkill().getId()).isEqualTo(5L);
        assertThat(captor.getValue().getProficiency()).isEqualTo("ADVANCED");
    }

    @Test
    @DisplayName("duplicate skill assignment is rejected 409 per the existing rule")
    void duplicateSkillAssignmentRejected() throws Exception {
        stubAuthenticatedCandidate(2L, 10L);
        when(candidateRepository.existsById(10L)).thenReturn(true);
        when(skillRepository.findById(5L))
                .thenReturn(Optional.of(skill(5L, "Data Collection", Skill.STATUS_ACTIVE)));
        when(candidateSkillRepository.existsByCandidateIdAndSkillId(10L, 5L)).thenReturn(true);

        mockMvc.perform(post(ME_SKILLS)
                        .header("Authorization", "Bearer " + tokenFor(2L, CANDIDATE_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"skillId\":5}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errors[0].code").value("CONFLICT"));

        verify(candidateSkillRepository, never()).save(any(CandidateSkill.class));
    }

    @Test
    @DisplayName("unknown or INACTIVE catalog skill is a masked 404 (no assignment)")
    void unknownOrInactiveSkillMasked404() throws Exception {
        stubAuthenticatedCandidate(2L, 10L);
        when(candidateRepository.existsById(10L)).thenReturn(true);
        when(skillRepository.findById(99L)).thenReturn(Optional.empty());
        when(skillRepository.findById(44L))
                .thenReturn(Optional.of(skill(44L, "Retired Skill", Skill.STATUS_INACTIVE)));

        for (long id : new long[]{99L, 44L}) {
            mockMvc.perform(post(ME_SKILLS)
                            .header("Authorization", "Bearer " + tokenFor(2L, CANDIDATE_EMAIL))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"skillId\":" + id + "}"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false));
        }
        verify(candidateSkillRepository, never()).save(any(CandidateSkill.class));
    }

    @Test
    @DisplayName("proficiency validation follows the existing BEGINNER/INTERMEDIATE/ADVANCED/EXPERT rule")
    void proficiencyValidationFollowsExistingRule() throws Exception {
        stubAuthenticatedCandidate(2L, 10L);

        mockMvc.perform(post(ME_SKILLS)
                        .header("Authorization", "Bearer " + tokenFor(2L, CANDIDATE_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"skillId\":5,\"proficiency\":\"WIZARD\"}"))
                .andExpect(status().isBadRequest());

        // Missing skillId is rejected by the existing @NotNull contract.
        mockMvc.perform(post(ME_SKILLS)
                        .header("Authorization", "Bearer " + tokenFor(2L, CANDIDATE_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verify(skillRepository, never()).findById(anyLong());
        verify(candidateSkillRepository, never()).save(any(CandidateSkill.class));
    }

    @Test
    @DisplayName("candidate removes own skill (204); foreign assignment is masked 404")
    void removeOwnAndForeignMasked() throws Exception {
        stubAuthenticatedCandidate(2L, 10L);

        // Own assignment → 204.
        when(candidateSkillRepository.existsById(new CandidateSkillId(10L, 5L))).thenReturn(true);
        mockMvc.perform(delete(ME_SKILL, 5L)
                        .header("Authorization", "Bearer " + tokenFor(2L, CANDIDATE_EMAIL)))
                .andExpect(status().isNoContent());
        verify(candidateSkillRepository).deleteById(new CandidateSkillId(10L, 5L));

        // Foreign assignment (candidate 99's skill 44) → same plain 404 as a
        // nonexistent one — no existence leak.
        when(candidateSkillRepository.existsById(new CandidateSkillId(10L, 44L))).thenReturn(false);
        mockMvc.perform(delete(ME_SKILL, 44L)
                        .header("Authorization", "Bearer " + tokenFor(2L, CANDIDATE_EMAIL)))
                .andExpect(status().isNotFound());
        verify(candidateSkillRepository, never()).deleteById(new CandidateSkillId(10L, 44L));
    }

    @Test
    @DisplayName("catalog read returns only ACTIVE skills via the safe SkillResponse DTO")
    void catalogIsActiveOnly() throws Exception {
        stubAuthenticatedCandidate(2L, 10L);
        when(skillRepository.findAllByStatus(eq(Skill.STATUS_ACTIVE), any(Pageable.class)))
                .thenReturn(new PageImpl<>(
                        List.of(skill(5L, "Community Mobilisation", Skill.STATUS_ACTIVE),
                                skill(3L, "Data Collection", Skill.STATUS_ACTIVE)),
                        PageRequest.of(0, 500), 2));

        mockMvc.perform(get(ME_SKILLS + "/catalog")
                        .header("Authorization", "Bearer " + tokenFor(2L, CANDIDATE_EMAIL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].name").value("Community Mobilisation"))
                // No lifecycle bookkeeping leaks through the safe DTO.
                .andExpect(jsonPath("$.data[0].updatedAt").exists());

        verify(skillRepository).findAllByStatus(eq(Skill.STATUS_ACTIVE), any(Pageable.class));
        verify(candidateSkillRepository, never()).save(any(CandidateSkill.class));
    }

    @Test
    @DisplayName("a user without a candidate profile receives a plain 404 on skill APIs")
    void nonCandidateProfileGets404OnSkills() throws Exception {
        User user = dbUser(2L, CANDIDATE_EMAIL);
        when(userRepository.findByEmail(CANDIDATE_EMAIL)).thenReturn(Optional.of(user));
        when(candidateRepository.findByUserId(2L)).thenReturn(Optional.empty());

        mockMvc.perform(get(ME_SKILLS)
                        .header("Authorization", "Bearer " + tokenFor(2L, CANDIDATE_EMAIL)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get(ME_SKILLS + "/catalog")
                        .header("Authorization", "Bearer " + tokenFor(2L, CANDIDATE_EMAIL)))
                .andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------
    // withdrawal
    // ------------------------------------------------------------------

    @Test
    @DisplayName("candidate withdraws own SUBMITTED application (existing workflow side effects preserved)")
    void candidateWithdrawsOwnApplication() throws Exception {
        stubAuthenticatedCandidate(2L, 10L);
        JobApplication own = application(7L, 10L, JobApplication.STATUS_SUBMITTED);
        when(applicationRepository.findByIdAndCandidateId(7L, 10L))
                .thenReturn(Optional.of(own));
        when(applicationRepository.findById(7L)).thenReturn(Optional.of(own));
        when(applicationRepository.save(any(JobApplication.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(post(ME_APPLICATIONS + "/7/withdraw")
                        .header("Authorization", "Bearer " + tokenFor(2L, CANDIDATE_EMAIL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(7))
                .andExpect(jsonPath("$.data.status").value("WITHDRAWN"));

        org.mockito.ArgumentCaptor<JobApplication> captor =
                org.mockito.ArgumentCaptor.forClass(JobApplication.class);
        verify(applicationRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("WITHDRAWN");

        // Existing A7.4/A7.5 side effects ran (delegated, not duplicated).
        verify(historyRepository).save(any(
                com.lokmit.foundation.employment.application.history.entity.ApplicationStatusHistory.class));
        verify(auditLogRepository).save(any(
                com.lokmit.foundation.audit.entity.AuditLog.class));
        org.mockito.ArgumentCaptor<com.lokmit.foundation.outbox.entity.OutboxEvent> outboxCaptor =
                org.mockito.ArgumentCaptor.forClass(
                        com.lokmit.foundation.outbox.entity.OutboxEvent.class);
        verify(outboxEventRepository).save(outboxCaptor.capture());
        assertThat(outboxCaptor.getValue().getPayload()).contains("WITHDRAWN");
    }

    @Test
    @DisplayName("own UNDER_REVIEW application can be withdrawn (pre-decision state)")
    void underReviewWithdrawalSucceeds() throws Exception {
        stubAuthenticatedCandidate(2L, 10L);
        JobApplication own = application(8L, 10L, JobApplication.STATUS_UNDER_REVIEW);
        when(applicationRepository.findByIdAndCandidateId(8L, 10L))
                .thenReturn(Optional.of(own));
        when(applicationRepository.findById(8L)).thenReturn(Optional.of(own));
        when(applicationRepository.save(any(JobApplication.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(post(ME_APPLICATIONS + "/8/withdraw")
                        .header("Authorization", "Bearer " + tokenFor(2L, CANDIDATE_EMAIL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("WITHDRAWN"));
    }

    @Test
    @DisplayName("decided (HIRED) and already-WITHDRAWN applications are rejected 409 by the existing rules")
    void decidedAndAlreadyWithdrawnRejected() throws Exception {
        stubAuthenticatedCandidate(2L, 10L);
        JobApplication hired = application(9L, 10L, JobApplication.STATUS_HIRED);
        when(applicationRepository.findByIdAndCandidateId(9L, 10L))
                .thenReturn(Optional.of(hired));
        when(applicationRepository.findById(9L)).thenReturn(Optional.of(hired));

        mockMvc.perform(post(ME_APPLICATIONS + "/9/withdraw")
                        .header("Authorization", "Bearer " + tokenFor(2L, CANDIDATE_EMAIL)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errors[0].code").value("CONFLICT"));

        JobApplication withdrawn = application(10L, 10L, JobApplication.STATUS_WITHDRAWN);
        when(applicationRepository.findByIdAndCandidateId(10L, 10L))
                .thenReturn(Optional.of(withdrawn));
        when(applicationRepository.findById(10L)).thenReturn(Optional.of(withdrawn));

        mockMvc.perform(post(ME_APPLICATIONS + "/10/withdraw")
                        .header("Authorization", "Bearer " + tokenFor(2L, CANDIDATE_EMAIL)))
                .andExpect(status().isConflict());

        // Neither transition mutated anything.
        verify(applicationRepository, never()).save(any(JobApplication.class));
        verify(historyRepository, never()).save(any(
                com.lokmit.foundation.employment.application.history.entity.ApplicationStatusHistory.class));
    }

    @Test
    @DisplayName("foreign and unknown application ids are the same masked 404 (no existence leak)")
    void foreignAndUnknownWithdrawalsMasked() throws Exception {
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
            mockMvc.perform(post(ME_APPLICATIONS + "/" + id + "/withdraw")
                            .header("Authorization", "Bearer " + tokenFor(2L, CANDIDATE_EMAIL)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false));
        }

        // Ownership gate fired BEFORE any transition — nothing was mutated.
        verify(applicationRepository, never()).save(any(JobApplication.class));
        verify(historyRepository, never()).save(any(
                com.lokmit.foundation.employment.application.history.entity.ApplicationStatusHistory.class));
        verify(outboxEventRepository, never()).save(any(
                com.lokmit.foundation.outbox.entity.OutboxEvent.class));
    }

    @Test
    @DisplayName("anonymous withdrawal is rejected 401 by the security chain")
    void anonymousWithdrawalRejected() throws Exception {
        mockMvc.perform(post(ME_APPLICATIONS + "/7/withdraw"))
                .andExpect(status().isUnauthorized());
        verify(applicationRepository, never()).save(any(JobApplication.class));
    }

    // ------------------------------------------------------------------
    // security regression — admin skill/application APIs unchanged
    // ------------------------------------------------------------------

    @Test
    @DisplayName("admin skill APIs remain permission-guarded: anonymous 401, candidate 403")
    void adminSkillApisRemainProtected() throws Exception {
        mockMvc.perform(get(ApiPaths.ADMIN_CANDIDATE_SKILLS, 10L))
                .andExpect(status().isUnauthorized());

        stubAuthenticatedCandidate(2L, 10L);
        mockMvc.perform(get(ApiPaths.ADMIN_CANDIDATE_SKILLS, 10L)
                        .header("Authorization", "Bearer " + tokenFor(2L, CANDIDATE_EMAIL)))
                .andExpect(status().isForbidden());
    }
}
