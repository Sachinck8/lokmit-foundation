package com.lokmit.foundation.employment.controller;

import com.lokmit.foundation.common.constants.ApiPaths;
import com.lokmit.foundation.employment.candidate.controller.AdminCandidateProfileController;
import com.lokmit.foundation.employment.candidateskill.dto.CandidateSkillResponse;
import com.lokmit.foundation.employment.candidateskill.service.CandidateSkillService;
import com.lokmit.foundation.employment.education.dto.CandidateEducationResponse;
import com.lokmit.foundation.employment.education.service.CandidateEducationService;
import com.lokmit.foundation.employment.experience.dto.CandidateExperienceResponse;
import com.lokmit.foundation.employment.experience.service.CandidateExperienceService;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A18 security matrix for the read-only admin candidate-profile review
 * surfaces and the applications DTO identity enrichment, exercised through
 * the REAL Spring Security filter chain (same setup as the A7-A14
 * matrices).
 *
 * <p>Coverage: admin (candidates:manage) reads all three sections;
 * anonymous 401; candidate-role 403 on every section; unknown candidate
 * 404; the applications candidate summary exposes the review identity
 * (name + email) while linked-user security internals never leak.</p>
 */
@WebMvcTest(controllers = {AdminCandidateProfileController.class,
        com.lokmit.foundation.employment.application.controller.ApplicationController.class})
@Import({SecurityConfig.class, CorsConfig.class, JwtKeyConfig.class,
        JwtTokenProvider.class, CustomUserDetailsService.class, SecurityUtils.class,
        CandidateSkillService.class, CandidateEducationService.class,
        CandidateExperienceService.class,
        com.lokmit.foundation.employment.application.service.ApplicationService.class,
        com.lokmit.foundation.employment.application.history.service.ApplicationStatusHistoryService.class,
        com.lokmit.foundation.audit.service.AuditLogService.class,
        com.lokmit.foundation.outbox.service.OutboxService.class,
        com.lokmit.foundation.outbox.service.OutboxEventProcessor.class,
        com.lokmit.foundation.outbox.config.OutboxRelayConfig.class})
@AutoConfigureMockMvc
class AdminCandidateProfileReadSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private com.lokmit.foundation.employment.candidateskill.repository.CandidateSkillRepository candidateSkillRepository;

    @MockitoBean
    private com.lokmit.foundation.employment.education.repository.CandidateEducationRepository educationRepository;

    @MockitoBean
    private com.lokmit.foundation.employment.experience.repository.CandidateExperienceRepository experienceRepository;

    @MockitoBean
    private com.lokmit.foundation.employment.skill.repository.SkillRepository skillRepository;

    @MockitoBean
    private com.lokmit.foundation.employment.candidate.repository.CandidateRepository candidateRepository;

    @MockitoBean
    private com.lokmit.foundation.employment.application.repository.JobApplicationRepository applicationRepository;

    @MockitoBean
    private com.lokmit.foundation.employment.application.history.repository.ApplicationStatusHistoryRepository historyRepository;

    @MockitoBean
    private com.lokmit.foundation.audit.repository.AuditLogRepository auditLogRepository;

    @MockitoBean
    private com.lokmit.foundation.notification.repository.NotificationRepository notificationRepository;

    @MockitoBean
    private com.lokmit.foundation.outbox.repository.OutboxEventRepository outboxEventRepository;

    @MockitoBean
    private com.lokmit.foundation.employment.resume.repository.ResumeRepository resumeRepository;

    @MockitoBean
    private com.lokmit.foundation.employment.job.repository.JobRepository jobRepository;

    @MockitoBean
    private UserRepository userRepository;

    private static final String SKILLS = ApiPaths.ADMIN_CANDIDATES + "/7/skills";
    private static final String EDUCATIONS = ApiPaths.ADMIN_CANDIDATES + "/7/educations";
    private static final String EXPERIENCES = ApiPaths.ADMIN_CANDIDATES + "/7/experiences";
    private static final String APPLICATIONS = ApiPaths.ADMIN_APPLICATIONS;

    private static final String ADMIN_EMAIL = "admin@lokmitfoundation.org";
    private static final String CANDIDATE_EMAIL = "candidate@lokmitfoundation.org";

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void resetStubs() {
        reset(candidateSkillRepository, educationRepository, experienceRepository,
                candidateRepository, userRepository, skillRepository,
                applicationRepository, historyRepository, auditLogRepository,
                notificationRepository, outboxEventRepository, resumeRepository,
                jobRepository);
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

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

    private User dbUser(long id, String email, Role role) {
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
        user.setRoles(Set.of(role));
        return user;
    }

    private String adminAuth() {
        Role admin = role("ADMIN", "dashboard:view", "employment:manage", "candidates:manage");
        User adminUser = dbUser(2L, ADMIN_EMAIL, admin);
        when(userRepository.findByEmail(ADMIN_EMAIL)).thenReturn(Optional.of(adminUser));
        return "Bearer " + jwtTokenProvider.generateAccessToken(
                2L, ADMIN_EMAIL, List.of("ADMIN"));
    }

    private String candidateAuth() {
        Role candidateRole = role("CANDIDATE");
        User candidateUser = dbUser(3L, CANDIDATE_EMAIL, candidateRole);
        candidateUser.setUserType("CANDIDATE");
        when(userRepository.findByEmail(CANDIDATE_EMAIL)).thenReturn(Optional.of(candidateUser));
        return "Bearer " + jwtTokenProvider.generateAccessToken(
                3L, CANDIDATE_EMAIL, List.of("CANDIDATE"));
    }

    private CandidateSkillResponse skillResponse() {
        return CandidateSkillResponse.builder()
                .skillId(5L).skillName("Java").build();
    }

    private com.lokmit.foundation.employment.education.entity.CandidateEducation educationEntity() {
        com.lokmit.foundation.employment.education.entity.CandidateEducation e =
                new com.lokmit.foundation.employment.education.entity.CandidateEducation();
        e.setId(11L);
        e.setInstitution("Patna Science College");
        e.setDegree("B.Sc");
        e.setStartYear(2010);
        return e;
    }

    private com.lokmit.foundation.employment.experience.entity.CandidateExperience experienceEntity() {
        com.lokmit.foundation.employment.experience.entity.CandidateExperience e =
                new com.lokmit.foundation.employment.experience.entity.CandidateExperience();
        e.setId(21L);
        e.setCompanyName("Hope Works");
        e.setJobTitle("Developer");
        e.setStartDate(java.time.LocalDate.of(2015, 1, 1));
        return e;
    }

    // ------------------------------------------------------------------
    // admin success paths
    // ------------------------------------------------------------------

    @Test
    @DisplayName("admin reads candidate skills / educations / experiences")
    void adminReadsAllSections() throws Exception {
        String auth = adminAuth();
        when(candidateRepository.existsById(7L)).thenReturn(true);
        when(candidateSkillRepository.findByCandidateId(7L))
                .thenReturn(List.of());
        when(educationRepository.findByCandidateIdOrderByCreatedAtDesc(7L))
                .thenReturn(List.of());
        when(experienceRepository.findByCandidateIdOrderByCreatedAtDesc(7L))
                .thenReturn(List.of());

        mockMvc.perform(get(SKILLS).header("Authorization", auth))
                .andExpect(status().isOk());
        mockMvc.perform(get(EDUCATIONS).header("Authorization", auth))
                .andExpect(status().isOk());
        mockMvc.perform(get(EXPERIENCES).header("Authorization", auth))
                .andExpect(status().isOk());

        verify(candidateSkillRepository).findByCandidateId(7L);
        verify(educationRepository).findByCandidateIdOrderByCreatedAtDesc(7L);
        verify(experienceRepository).findByCandidateIdOrderByCreatedAtDesc(7L);
    }

    @Test
    @DisplayName("admin review sections return mapped DTO content (no candidate blob leakage)")
    void adminReadsMappedContent() throws Exception {
        String auth = adminAuth();
        when(candidateRepository.existsById(7L)).thenReturn(true);
        when(educationRepository.findByCandidateIdOrderByCreatedAtDesc(7L))
                .thenReturn(List.of(educationEntity()));
        when(experienceRepository.findByCandidateIdOrderByCreatedAtDesc(7L))
                .thenReturn(List.of(experienceEntity()));
        when(candidateSkillRepository.findByCandidateId(7L)).thenReturn(List.of());

        String body = mockMvc.perform(get(EDUCATIONS).header("Authorization", auth))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(body).contains("Patna Science College").doesNotContain("candidate");

        mockMvc.perform(get(EXPERIENCES).header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].companyName").value("Hope Works"));
    }

    // ------------------------------------------------------------------
    // authorization matrix
    // ------------------------------------------------------------------

    @Test
    @DisplayName("anonymous user is rejected on every section (401)")
    void anonymousIsRejected() throws Exception {
        mockMvc.perform(get(SKILLS)).andExpect(status().isUnauthorized());
        mockMvc.perform(get(EDUCATIONS)).andExpect(status().isUnauthorized());
        mockMvc.perform(get(EXPERIENCES)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("candidate-role user is rejected on every section (403)")
    void candidateIsForbidden() throws Exception {
        String auth = candidateAuth();
        mockMvc.perform(get(SKILLS).header("Authorization", auth))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(EDUCATIONS).header("Authorization", auth))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(EXPERIENCES).header("Authorization", auth))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("unknown candidate returns 404 (no empty-list leak)")
    void unknownCandidateIsNotFound() throws Exception {
        String auth = adminAuth();
        when(candidateRepository.existsById(7L)).thenReturn(false);

        mockMvc.perform(get(SKILLS).header("Authorization", auth))
                .andExpect(status().isNotFound());
        mockMvc.perform(get(EDUCATIONS).header("Authorization", auth))
                .andExpect(status().isNotFound());
        mockMvc.perform(get(EXPERIENCES).header("Authorization", auth))
                .andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------
    // A18 candidate identity enrichment (ApplicationResponse)
    // ------------------------------------------------------------------

    @Test
    @DisplayName("applications candidate summary exposes review identity without security internals")
    void applicationSummaryExposesReviewIdentityOnly() throws Exception {
        Role admin = role("ADMIN", "employment:manage");
        User adminUser = dbUser(2L, ADMIN_EMAIL, admin);
        when(userRepository.findByEmail(ADMIN_EMAIL)).thenReturn(Optional.of(adminUser));
        String auth = "Bearer " + jwtTokenProvider.generateAccessToken(
                2L, ADMIN_EMAIL, List.of("ADMIN"));

        User linked = new User();
        linked.setId(95L);
        linked.setEmail("sunita.dev@example.org");
        linked.setFullName("Sunita Devi");
        linked.setUserType("CANDIDATE");
        linked.setStatus("ACTIVE");
        linked.setEmailVerified(true);
        linked.setPasswordHash("$2a$10$secret-hash-should-never-leak");
        linked.setFailedLoginAttempts(7);
        linked.setLockedUntil(OffsetDateTime.now().plusMinutes(10));
        linked.setCreatedAt(OffsetDateTime.now());
        linked.setUpdatedAt(OffsetDateTime.now());

        com.lokmit.foundation.employment.candidate.entity.Candidate candidate =
                new com.lokmit.foundation.employment.candidate.entity.Candidate();
        candidate.setId(20L);
        candidate.setUser(linked);
        candidate.setPhone("9841000020");
        candidate.setCurrentLocation("Kathmandu");
        candidate.setSummary("Backend developer");
        candidate.setExpectedSalaryMin(new java.math.BigDecimal("800.00"));
        candidate.setExpectedSalaryMax(new java.math.BigDecimal("1200.00"));
        candidate.setAvailabilityStatus("ACTIVELY_LOOKING");
        candidate.setCreatedAt(OffsetDateTime.now());
        candidate.setUpdatedAt(OffsetDateTime.now());

        com.lokmit.foundation.employment.employer.entity.Employer employer =
                new com.lokmit.foundation.employment.employer.entity.Employer();
        employer.setId(1L);
        employer.setCompanyName("Acme Ltd");
        employer.setStatus("ACTIVE");
        employer.setCreatedAt(OffsetDateTime.now());
        employer.setUpdatedAt(OffsetDateTime.now());

        com.lokmit.foundation.employment.job.entity.Job job =
                new com.lokmit.foundation.employment.job.entity.Job();
        job.setId(10L);
        job.setEmployer(employer);
        job.setSlug("java-dev-10");
        job.setTitle("Java Developer");
        job.setDescription("Build services");
        job.setEmploymentType("FULL_TIME");
        job.setWorkMode("HYBRID");
        job.setSalaryMin(new java.math.BigDecimal("1000.00"));
        job.setSalaryMax(new java.math.BigDecimal("2000.00"));
        job.setSalaryCurrency("INR");
        job.setStatus("PUBLISHED");
        job.setPublishedAt(OffsetDateTime.now());
        job.setCreatedAt(OffsetDateTime.now());
        job.setUpdatedAt(OffsetDateTime.now());

        com.lokmit.foundation.employment.application.entity.JobApplication app =
                new com.lokmit.foundation.employment.application.entity.JobApplication();
        app.setId(30L);
        app.setJob(job);
        app.setCandidate(candidate);
        app.setStatus("SUBMITTED");
        app.setAppliedAt(OffsetDateTime.now());
        app.setCreatedAt(OffsetDateTime.now());
        app.setUpdatedAt(OffsetDateTime.now());

        when(applicationRepository.findById(30L)).thenReturn(Optional.of(app));

        String body = mockMvc.perform(get(APPLICATIONS + "/30")
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Review identity is now intentionally present.
        assertThat(body).contains("Sunita Devi");
        assertThat(body).contains("sunita.dev@example.org");
        assertThat(body).contains("Backend developer");
        assertThat(body).contains("expectedSalaryMin");
        // Security internals still never leak.
        assertThat(body)
                .doesNotContain("secret-hash")
                .doesNotContain("failedLogin")
                .doesNotContain("lockedUntil")
                .doesNotContain("refreshToken")
                .doesNotContain("userType")
                .doesNotContain("\"roles\"");
    }
}
