package com.lokmit.foundation.employment.controller;

import com.lokmit.foundation.common.constants.ApiPaths;
import com.lokmit.foundation.employment.candidate.repository.CandidateRepository;
import com.lokmit.foundation.employment.employer.entity.Employer;
import com.lokmit.foundation.employment.employer.repository.EmployerRepository;
import com.lokmit.foundation.employment.job.controller.JobController;
import com.lokmit.foundation.employment.job.controller.JobSkillController;
import com.lokmit.foundation.employment.job.dto.JobResponse;
import com.lokmit.foundation.employment.job.entity.Job;
import com.lokmit.foundation.employment.job.entity.JobSkill;
import com.lokmit.foundation.employment.job.repository.JobRepository;
import com.lokmit.foundation.employment.job.repository.JobSkillRepository;
import com.lokmit.foundation.employment.job.service.JobService;
import com.lokmit.foundation.employment.job.service.JobSkillService;
import com.lokmit.foundation.employment.jobcategory.entity.JobCategory;
import com.lokmit.foundation.employment.jobcategory.repository.JobCategoryRepository;
import com.lokmit.foundation.employment.skill.entity.Skill;
import com.lokmit.foundation.employment.skill.repository.SkillRepository;
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
 * A7.2 security and behavior matrix exercised through the REAL Spring
 * Security filter chain (real JwtAuthenticationFilter, real JwtTokenProvider,
 * real CustomUserDetailsService with mocked repositories — identical to the
 * A1–A7.1 matrix setups).
 *
 * <p>Coverage: anonymous 401 on every family; MODERATOR/EDITOR/CANDIDATE-role
 * 403; ADMIN and SUPER_ADMIN success; spoofed JWT role claims rejected;
 * CRUD + validation + duplicate slug 409 + unknown employer/category 404;
 * lifecycle DRAFT → PUBLISHED → CLOSED → ARCHIVED with published_at stamping,
 * invalid transitions rejected, archived terminal; job-skill assign/list/
 * remove with duplicate 409 and missing 404s; pagination defaults/cap;
 * DTO safety (no employer-user security internals).</p>
 */
@WebMvcTest(controllers = {JobController.class, JobSkillController.class})
@Import({SecurityConfig.class, CorsConfig.class, JwtKeyConfig.class,
        JwtTokenProvider.class, CustomUserDetailsService.class, SecurityUtils.class,
        JobService.class, JobSkillService.class})
@AutoConfigureMockMvc
class AdminJobsSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JobRepository jobRepository;

    @MockitoBean
    private JobSkillRepository jobSkillRepository;

    @MockitoBean
    private EmployerRepository employerRepository;

    @MockitoBean
    private JobCategoryRepository jobCategoryRepository;

    @MockitoBean
    private SkillRepository skillRepository;

    @MockitoBean
    private CandidateRepository candidateRepository;

    @MockitoBean
    private UserRepository userRepository;

    private static final String JOBS = ApiPaths.ADMIN_JOBS;

    private static final String SUPER_ADMIN_EMAIL = "superadmin@lokmitfoundation.org";
    private static final String ADMIN_EMAIL = "admin@lokmitfoundation.org";
    private static final String MODERATOR_EMAIL = "moderator@lokmitfoundation.org";
    private static final String EDITOR_EMAIL = "editor@lokmitfoundation.org";
    private static final String CANDIDATE_EMAIL = "candidate@lokmitfoundation.org";

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void resetStubs() {
        reset(jobRepository, jobSkillRepository, employerRepository,
                jobCategoryRepository, skillRepository, candidateRepository,
                userRepository);
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

    private Employer dbEmployer(long id) {
        Employer e = new Employer();
        e.setId(id);
        e.setUser(new User());
        e.setCompanyName("Acme Ltd");
        e.setVerificationStatus("VERIFIED");
        e.setStatus("ACTIVE");
        e.setCreatedAt(OffsetDateTime.now());
        e.setUpdatedAt(OffsetDateTime.now());
        return e;
    }

    private JobCategory dbCategory(long id) {
        JobCategory c = new JobCategory();
        c.setId(id);
        c.setName("Engineering");
        c.setSlug("engineering");
        c.setDisplayOrder(0);
        c.setStatus("ACTIVE");
        c.setCreatedAt(OffsetDateTime.now());
        c.setUpdatedAt(OffsetDateTime.now());
        return c;
    }

    private Skill dbSkill(long id, String name) {
        Skill s = new Skill();
        s.setId(id);
        s.setName(name);
        s.setStatus("ACTIVE");
        s.setCreatedAt(OffsetDateTime.now());
        s.setUpdatedAt(OffsetDateTime.now());
        return s;
    }

    private Job dbJob(long id, Employer employer, JobCategory category, String status) {
        Job job = new Job();
        job.setId(id);
        job.setEmployer(employer);
        job.setCategory(category);
        job.setSlug("java-dev-" + id);
        job.setTitle("Java Developer");
        job.setDescription("Build backend services");
        job.setEmploymentType("FULL_TIME");
        job.setWorkMode("HYBRID");
        job.setWorkLocation("Kathmandu");
        job.setSalaryMin(new BigDecimal("1000.00"));
        job.setSalaryMax(new BigDecimal("2000.00"));
        job.setSalaryCurrency("INR");
        job.setOpenings(2);
        job.setStatus(status);
        job.setApplicationDeadline(LocalDate.of(2026, 12, 31));
        job.setPublishedAt("PUBLISHED".equals(status) || "CLOSED".equals(status)
                ? OffsetDateTime.now() : null);
        job.setCreatedAt(OffsetDateTime.now());
        job.setUpdatedAt(OffsetDateTime.now());
        return job;
    }

    private JobSkill dbJobSkill(Job job, Skill skill) {
        JobSkill js = new JobSkill();
        js.setJob(job);
        js.setSkill(skill);
        return js;
    }

    private String createBody() {
        return """
                {"slug":"senior-java-dev","title":"Senior Java Developer","description":"Build services","employerId":1,"categoryId":5,"employmentType":"FULL_TIME","workMode":"REMOTE","workLocation":"Kathmandu","salaryMin":1500.00,"salaryMax":3000.00,"salaryCurrency":"INR","openings":3,"applicationDeadline":"2026-12-31"}
                """;
    }

    // ------------------------------------------------------------------
    // A. Anonymous → 401 on every endpoint family
    // ------------------------------------------------------------------

    @Test
    @DisplayName("anonymous access is rejected with 401 on all job namespaces")
    void anonymousIs401Everywhere() throws Exception {
        mockMvc.perform(get(JOBS)).andExpect(status().isUnauthorized());
        mockMvc.perform(get(JOBS + "/1")).andExpect(status().isUnauthorized());
        mockMvc.perform(get(JOBS + "/1/skills")).andExpect(status().isUnauthorized());
        mockMvc.perform(post(JOBS).contentType(MediaType.APPLICATION_JSON)
                        .content(createBody()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(patch(JOBS + "/1").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"T\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post(JOBS + "/1/publish")).andExpect(status().isUnauthorized());
        mockMvc.perform(post(JOBS + "/1/close")).andExpect(status().isUnauthorized());
        mockMvc.perform(post(JOBS + "/1/archive")).andExpect(status().isUnauthorized());
        mockMvc.perform(delete(JOBS + "/1")).andExpect(status().isUnauthorized());
        mockMvc.perform(post(JOBS + "/1/skills/2")).andExpect(status().isUnauthorized());
        mockMvc.perform(delete(JOBS + "/1/skills/2")).andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------
    // B. Permission matrix
    // ------------------------------------------------------------------

    @Test
    @DisplayName("MODERATOR and EDITOR (no employment:manage) get 403 on all job namespaces")
    void moderatorAndEditorGet403() throws Exception {
        Role moderator = role("MODERATOR", "jobs:moderate", "messages:manage");
        givenDbUser(dbUser(3L, MODERATOR_EMAIL, moderator));
        String moderatorToken = "Bearer " + jwtTokenProvider.generateAccessToken(
                3L, MODERATOR_EMAIL, List.of("MODERATOR"));

        mockMvc.perform(get(JOBS).header("Authorization", moderatorToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(JOBS).header("Authorization", moderatorToken)
                        .contentType(MediaType.APPLICATION_JSON).content(createBody()))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete(JOBS + "/1").header("Authorization", moderatorToken))
                .andExpect(status().isForbidden());

        Role editor = role("EDITOR", "content:manage", "downloads:manage");
        givenDbUser(dbUser(4L, EDITOR_EMAIL, editor));
        String editorToken = "Bearer " + jwtTokenProvider.generateAccessToken(
                4L, EDITOR_EMAIL, List.of("EDITOR"));

        mockMvc.perform(get(JOBS).header("Authorization", editorToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(JOBS + "/1/publish").header("Authorization", editorToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("normal platform user (CANDIDATE role) gets 403 everywhere")
    void candidateRoleGets403() throws Exception {
        Role candidate = role("CANDIDATE");
        givenDbUser(dbUser(5L, CANDIDATE_EMAIL, candidate));
        String token = "Bearer " + jwtTokenProvider.generateAccessToken(
                5L, CANDIDATE_EMAIL, List.of("CANDIDATE"));

        mockMvc.perform(get(JOBS).header("Authorization", token))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(JOBS + "/1/skills").header("Authorization", token))
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

        mockMvc.perform(get(JOBS).header("Authorization", spoofed))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(JOBS).header("Authorization", spoofed)
                        .contentType(MediaType.APPLICATION_JSON).content(createBody()))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete(JOBS + "/1").header("Authorization", spoofed))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------
    // C. ADMIN and SUPER_ADMIN success paths
    // ------------------------------------------------------------------

    @Test
    @DisplayName("ADMIN can list, get, create, patch and delete jobs")
    void adminJobCrud() throws Exception {
        String auth = adminAuth();
        Employer employer = dbEmployer(1L);
        JobCategory category = dbCategory(5L);

        // list (empty)
        when(jobRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));
        mockMvc.perform(get(JOBS).header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.totalItems").value(0));

        // get existing — employer/category rendered as safe summaries
        when(jobRepository.findById(10L))
                .thenReturn(Optional.of(dbJob(10L, employer, category, "DRAFT")));
        mockMvc.perform(get(JOBS + "/10").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.slug").value("java-dev-10"))
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.employer.id").value(1))
                .andExpect(jsonPath("$.data.employer.companyName").value("Acme Ltd"))
                .andExpect(jsonPath("$.data.category.slug").value("engineering"))
                .andExpect(jsonPath("$.data.publishedAt").doesNotExist());

        // get missing → 404
        when(jobRepository.findById(99L)).thenReturn(Optional.empty());
        mockMvc.perform(get(JOBS + "/99").header("Authorization", auth))
                .andExpect(status().isNotFound());

        // create — starts DRAFT even if a client tries to smuggle a status
        when(jobRepository.existsBySlugIgnoreCase("senior-java-dev")).thenReturn(false);
        when(employerRepository.findById(1L)).thenReturn(Optional.of(employer));
        when(jobCategoryRepository.findById(5L)).thenReturn(Optional.of(category));
        when(jobRepository.saveAndFlush(any(Job.class))).thenAnswer(inv -> inv.getArgument(0));
        mockMvc.perform(post(JOBS).header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody().replace("\"openings\":3,",
                                "\"openings\":3,\"status\":\"PUBLISHED\",\"publishedAt\":\"2020-01-01T00:00:00Z\",")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.publishedAt").doesNotExist());

        // create with unknown employer → 404
        when(employerRepository.findById(777L)).thenReturn(Optional.empty());
        mockMvc.perform(post(JOBS).header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody().replace("\"employerId\":1", "\"employerId\":777")))
                .andExpect(status().isNotFound());

        // create with unknown category → 404
        when(jobCategoryRepository.findById(999L)).thenReturn(Optional.empty());
        mockMvc.perform(post(JOBS).header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody().replace("\"categoryId\":5", "\"categoryId\":999")))
                .andExpect(status().isNotFound());

        // duplicate slug → 409
        when(jobRepository.existsBySlugIgnoreCase("senior-java-dev")).thenReturn(true);
        mockMvc.perform(post(JOBS).header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content(createBody()))
                .andExpect(status().isConflict());

        // create with salaryMin > salaryMax → 400 (fresh slug, unique pre-check passes)
        when(jobRepository.existsBySlugIgnoreCase("bad-salary-job")).thenReturn(false);
        mockMvc.perform(post(JOBS).header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody()
                                .replace("senior-java-dev", "bad-salary-job")
                                .replace("\"salaryMin\":1500.00", "\"salaryMin\":5000.00")))
                .andExpect(status().isBadRequest());

        // patch — merges and validates the final salary pair
        Job existing = dbJob(10L, employer, category, "DRAFT");
        when(jobRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(jobRepository.save(any(Job.class))).thenAnswer(inv -> inv.getArgument(0));
        mockMvc.perform(patch(JOBS + "/10").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Lead Java Developer\",\"salaryMax\":4000.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Lead Java Developer"))
                .andExpect(jsonPath("$.data.salaryMax").value(4000.00));

        // patch cannot change status (status not a patchable field)
        mockMvc.perform(patch(JOBS + "/10").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"PUBLISHED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DRAFT"));

        // patch making min > max → 400
        mockMvc.perform(patch(JOBS + "/10").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"salaryMax\":500.00}"))
                .andExpect(status().isBadRequest());

        // delete → 204
        when(jobRepository.existsById(10L)).thenReturn(true);
        mockMvc.perform(delete(JOBS + "/10").header("Authorization", auth))
                .andExpect(status().isNoContent());
        verify(jobRepository).deleteById(10L);

        // delete missing → 404
        when(jobRepository.existsById(99L)).thenReturn(false);
        mockMvc.perform(delete(JOBS + "/99").header("Authorization", auth))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("SUPER_ADMIN can create and manage jobs end to end")
    void superAdminJobCrud() throws Exception {
        Role superAdmin = role("SUPER_ADMIN", "users:manage", "dashboard:view",
                "content:manage", "content:publish", "downloads:manage", "messages:manage",
                "jobs:manage", "jobs:moderate", "settings:manage", "employment:manage",
                "candidates:manage");
        givenDbUser(dbUser(1L, SUPER_ADMIN_EMAIL, superAdmin));
        String auth = "Bearer " + jwtTokenProvider.generateAccessToken(
                1L, SUPER_ADMIN_EMAIL, List.of("SUPER_ADMIN"));

        when(jobRepository.existsBySlugIgnoreCase("qa-lead")).thenReturn(false);
        when(employerRepository.findById(1L)).thenReturn(Optional.of(dbEmployer(1L)));
        when(jobCategoryRepository.findById(5L)).thenReturn(Optional.of(dbCategory(5L)));
        when(jobRepository.saveAndFlush(any(Job.class))).thenAnswer(inv -> inv.getArgument(0));
        mockMvc.perform(post(JOBS).header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody().replace("senior-java-dev", "qa-lead")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("DRAFT"));

        // list with filters + search params accepted (DB-side)
        when(jobRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenAnswer(inv -> new PageImpl<>(
                        List.of(dbJob(11L, dbEmployer(1L), null, "PUBLISHED")),
                        inv.getArgument(1, Pageable.class), 1));
        mockMvc.perform(get(JOBS).header("Authorization", auth)
                        .param("status", "PUBLISHED")
                        .param("employmentType", "FULL_TIME")
                        .param("workMode", "HYBRID")
                        .param("employerId", "1")
                        .param("search", "java"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalItems").value(1))
                .andExpect(jsonPath("$.data.items[0].status").value("PUBLISHED"))
                // null category must serialize safely, not error
                .andExpect(jsonPath("$.data.items[0].category").doesNotExist());
    }

    // ------------------------------------------------------------------
    // D. Lifecycle
    // ------------------------------------------------------------------

    @Test
    @DisplayName("lifecycle: publish stamps published_at, close preserves data, archive terminal")
    void lifecycleTransitions() throws Exception {
        String auth = adminAuth();
        Employer employer = dbEmployer(1L);

        // publish from DRAFT → PUBLISHED + published_at stamped
        Job draft = dbJob(10L, employer, null, "DRAFT");
        when(jobRepository.findById(10L)).thenReturn(Optional.of(draft));
        when(jobRepository.save(any(Job.class))).thenAnswer(inv -> inv.getArgument(0));
        mockMvc.perform(post(JOBS + "/10/publish").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.data.publishedAt").isNotEmpty());

        // publish again → 409
        Job published = dbJob(10L, employer, null, "PUBLISHED");
        when(jobRepository.findById(10L)).thenReturn(Optional.of(published));
        mockMvc.perform(post(JOBS + "/10/publish").header("Authorization", auth))
                .andExpect(status().isConflict());

        // publish from CLOSED → 400 (only DRAFT may publish)
        Job closedJob = dbJob(10L, employer, null, "CLOSED");
        when(jobRepository.findById(10L)).thenReturn(Optional.of(closedJob));
        mockMvc.perform(post(JOBS + "/10/publish").header("Authorization", auth))
                .andExpect(status().isBadRequest());

        // close from PUBLISHED → CLOSED (data preserved; re-stub with a
        // PUBLISHED job — the previous step's stub was a CLOSED job)
        when(jobRepository.findById(10L))
                .thenReturn(Optional.of(dbJob(10L, employer, null, "PUBLISHED")));
        mockMvc.perform(post(JOBS + "/10/close").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CLOSED"))
                .andExpect(jsonPath("$.data.description").isNotEmpty());

        // close from DRAFT → 400
        when(jobRepository.findById(12L))
                .thenReturn(Optional.of(dbJob(12L, employer, null, "DRAFT")));
        mockMvc.perform(post(JOBS + "/12/close").header("Authorization", auth))
                .andExpect(status().isBadRequest());

        // archive from CLOSED → ARCHIVED (terminal; re-stub the CLOSED job
        // because close() mutated only the in-memory copy above)
        when(jobRepository.findById(10L))
                .thenReturn(Optional.of(dbJob(10L, employer, null, "CLOSED")));
        mockMvc.perform(post(JOBS + "/10/archive").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ARCHIVED"));

        // archive again → 409
        Job archived = dbJob(10L, employer, null, "ARCHIVED");
        when(jobRepository.findById(10L)).thenReturn(Optional.of(archived));
        mockMvc.perform(post(JOBS + "/10/archive").header("Authorization", auth))
                .andExpect(status().isConflict());

        // archive from DRAFT → allowed (DRAFT/PUBLISHED/CLOSED may archive)
        when(jobRepository.findById(13L))
                .thenReturn(Optional.of(dbJob(13L, employer, null, "DRAFT")));
        mockMvc.perform(post(JOBS + "/13/archive").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ARCHIVED"));

        // lifecycle on missing job → 404
        when(jobRepository.findById(99L)).thenReturn(Optional.empty());
        mockMvc.perform(post(JOBS + "/99/publish").header("Authorization", auth))
                .andExpect(status().isNotFound());
        mockMvc.perform(post(JOBS + "/99/close").header("Authorization", auth))
                .andExpect(status().isNotFound());
        mockMvc.perform(post(JOBS + "/99/archive").header("Authorization", auth))
                .andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------
    // E. Validation → 400
    // ------------------------------------------------------------------

    @Test
    @DisplayName("validation failures return 400 across job namespaces")
    void validationFailuresAre400() throws Exception {
        String auth = adminAuth();

        // missing required fields
        mockMvc.perform(post(JOBS).header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"T\"}"))
                .andExpect(status().isBadRequest());

        // slug format
        mockMvc.perform(post(JOBS).header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody().replace("senior-java-dev", "Bad Slug!")))
                .andExpect(status().isBadRequest());

        // slug over 180 chars
        mockMvc.perform(post(JOBS).header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody().replace("senior-java-dev", "s".repeat(181))))
                .andExpect(status().isBadRequest());

        // invalid employment type
        mockMvc.perform(post(JOBS).header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody()
                                .replace("\"employmentType\":\"FULL_TIME\"", "\"employmentType\":\"PERMANENT\"")))
                .andExpect(status().isBadRequest());

        // invalid work mode
        mockMvc.perform(post(JOBS).header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody()
                                .replace("\"workMode\":\"REMOTE\"", "\"workMode\":\"METaverse\"")))
                .andExpect(status().isBadRequest());

        // openings < 1
        mockMvc.perform(post(JOBS).header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody().replace("\"openings\":3", "\"openings\":0")))
                .andExpect(status().isBadRequest());

        // salary currency over 8 chars
        mockMvc.perform(post(JOBS).header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody()
                                .replace("\"salaryCurrency\":\"INR\"", "\"salaryCurrency\":\"INDIAN_RUPEE\"")))
                .andExpect(status().isBadRequest());

        // job skill POST requires no body — but unknown proficiency-style body is ignored;
        // wrong-verb checks are covered in lifecycle/delete tests.
    }

    // ------------------------------------------------------------------
    // F. Job ↔ skills
    // ------------------------------------------------------------------

    @Test
    @DisplayName("job skill assignment: list, add, duplicate 409, unknown 404, remove")
    void jobSkillAssignments() throws Exception {
        String auth = adminAuth();
        Job job = dbJob(10L, dbEmployer(1L), null, "DRAFT");
        Skill java = dbSkill(1L, "Java");
        Skill spring = dbSkill(2L, "Spring Boot");

        // unknown job → 404
        when(jobRepository.existsById(77L)).thenReturn(false);
        mockMvc.perform(get(JOBS + "/77/skills").header("Authorization", auth))
                .andExpect(status().isNotFound());
        mockMvc.perform(post(JOBS + "/77/skills").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"skillId\":1}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete(JOBS + "/77/skills/1").header("Authorization", auth))
                .andExpect(status().isNotFound());

        // list assignments
        when(jobRepository.existsById(10L)).thenReturn(true);
        when(jobSkillRepository.findByJobId(10L))
                .thenReturn(List.of(dbJobSkill(job, spring), dbJobSkill(job, java)));
        mockMvc.perform(get(JOBS + "/10/skills").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].skillName").value("Java"))
                .andExpect(jsonPath("$.data[1].skillName").value("Spring Boot"));

        // add (skill id in the JSON body, per the A7.1 candidate-skill convention)
        when(skillRepository.findById(1L)).thenReturn(Optional.of(java));
        when(jobRepository.getReferenceById(10L)).thenReturn(job);
        when(jobSkillRepository.existsByJobIdAndSkillId(10L, 1L)).thenReturn(false);
        when(jobSkillRepository.saveAndFlush(any(JobSkill.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        mockMvc.perform(post(JOBS + "/10/skills").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"skillId\":1}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.skillId").value(1))
                .andExpect(jsonPath("$.data.skillName").value("Java"));

        // duplicate → 409
        when(jobSkillRepository.existsByJobIdAndSkillId(10L, 1L)).thenReturn(true);
        mockMvc.perform(post(JOBS + "/10/skills").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"skillId\":1}"))
                .andExpect(status().isConflict());

        // unknown skill → 404
        when(skillRepository.findById(99L)).thenReturn(Optional.empty());
        mockMvc.perform(post(JOBS + "/10/skills").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"skillId\":99}"))
                .andExpect(status().isNotFound());

        // remove → 204
        when(jobSkillRepository.existsById(any())).thenReturn(true);
        mockMvc.perform(delete(JOBS + "/10/skills/1").header("Authorization", auth))
                .andExpect(status().isNoContent());

        // remove missing pair → 404
        when(jobSkillRepository.existsById(any())).thenReturn(false);
        mockMvc.perform(delete(JOBS + "/10/skills/999").header("Authorization", auth))
                .andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------
    // G. Pagination
    // ------------------------------------------------------------------

    @Test
    @DisplayName("pagination defaults and page-size cap behave as project convention")
    void paginationBehavior() throws Exception {
        String auth = adminAuth();

        when(jobRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenAnswer(inv -> new PageImpl<>(List.of(), inv.getArgument(1,
                        Pageable.class), 0));
        mockMvc.perform(get(JOBS).header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.page").value(0));

        // cap: size=200 → 400
        mockMvc.perform(get(JOBS).header("Authorization", auth).param("size", "200"))
                .andExpect(status().isBadRequest());

        // negative page → 400
        mockMvc.perform(get(JOBS).header("Authorization", auth).param("page", "-1"))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------
    // H. DTO safety
    // ------------------------------------------------------------------

    @Test
    @DisplayName("job responses never expose employer-user security fields")
    void dtoSafety() throws Exception {
        String auth = adminAuth();

        Employer employer = dbEmployer(1L);
        // poison the employer's linked user with security internals — none may leak
        User linked = new User();
        linked.setId(42L);
        linked.setEmail("hidden@lokmitfoundation.org");
        linked.setPasswordHash("$2a$10$secret-hash-should-never-leak");
        linked.setFailedLoginAttempts(5);
        linked.setLockedUntil(OffsetDateTime.now().plusMinutes(10));
        employer.setUser(linked);

        when(jobRepository.findById(10L))
                .thenReturn(Optional.of(dbJob(10L, employer, dbCategory(5L), "DRAFT")));

        String body = mockMvc.perform(get(JOBS + "/10").header("Authorization", auth))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(body)
                .doesNotContain("password")
                .doesNotContain("secret-hash")
                .doesNotContain("failedLogin")
                .doesNotContain("lockedUntil")
                .doesNotContain("refreshToken")
                .doesNotContain("hidden@lokmitfoundation.org")
                .doesNotContain("\"roles\"");
    }
}
