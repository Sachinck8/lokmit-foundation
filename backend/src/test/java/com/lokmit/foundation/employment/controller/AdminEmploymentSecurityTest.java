package com.lokmit.foundation.employment.controller;

import com.lokmit.foundation.common.constants.ApiPaths;
import com.lokmit.foundation.employment.candidate.controller.CandidateController;
import com.lokmit.foundation.employment.candidate.dto.CandidateCreateRequest;
import com.lokmit.foundation.employment.candidate.dto.CandidateResponse;
import com.lokmit.foundation.employment.candidate.dto.CandidateUpdateRequest;
import com.lokmit.foundation.employment.candidate.entity.Candidate;
import com.lokmit.foundation.employment.candidate.service.CandidateService;
import com.lokmit.foundation.employment.candidateskill.controller.CandidateSkillController;
import com.lokmit.foundation.employment.candidateskill.dto.CandidateSkillRequest;
import com.lokmit.foundation.employment.candidateskill.dto.CandidateSkillResponse;
import com.lokmit.foundation.employment.candidateskill.service.CandidateSkillService;
import com.lokmit.foundation.employment.employer.controller.EmployerController;
import com.lokmit.foundation.employment.employer.dto.EmployerCreateRequest;
import com.lokmit.foundation.employment.employer.dto.EmployerResponse;
import com.lokmit.foundation.employment.employer.dto.EmployerUpdateRequest;
import com.lokmit.foundation.employment.employer.entity.Employer;
import com.lokmit.foundation.employment.employer.service.EmployerService;
import com.lokmit.foundation.employment.jobcategory.controller.JobCategoryController;
import com.lokmit.foundation.employment.jobcategory.dto.JobCategoryCreateRequest;
import com.lokmit.foundation.employment.jobcategory.dto.JobCategoryResponse;
import com.lokmit.foundation.employment.jobcategory.dto.JobCategoryUpdateRequest;
import com.lokmit.foundation.employment.jobcategory.entity.JobCategory;
import com.lokmit.foundation.employment.jobcategory.service.JobCategoryService;
import com.lokmit.foundation.employment.skill.controller.SkillController;
import com.lokmit.foundation.employment.skill.dto.SkillCreateRequest;
import com.lokmit.foundation.employment.skill.dto.SkillResponse;
import com.lokmit.foundation.employment.skill.dto.SkillUpdateRequest;
import com.lokmit.foundation.employment.skill.entity.Skill;
import com.lokmit.foundation.employment.skill.service.SkillService;
import com.lokmit.foundation.employment.candidate.repository.CandidateRepository;
import com.lokmit.foundation.employment.candidateskill.entity.CandidateSkill;
import com.lokmit.foundation.employment.candidateskill.repository.CandidateSkillRepository;
import com.lokmit.foundation.employment.employer.repository.EmployerRepository;
import com.lokmit.foundation.employment.jobcategory.repository.JobCategoryRepository;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.MediaType;

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
 * A7.1 security and behavior matrix exercised through the REAL Spring
 * Security filter chain (real JwtAuthenticationFilter, real JwtTokenProvider,
 * real CustomUserDetailsService with mocked repositories — identical to the
 * A1–A6 matrix setups).
 *
 * <p>Coverage:</p>
 * <ul>
 *   <li>anonymous → 401 on every endpoint family</li>
 *   <li>permission matrix: MODERATOR/EDITOR (no employment/candidates:manage) → 403;
 *       ADMIN and SUPER_ADMIN → success; spoofed JWT role claims do not elevate</li>
 *   <li>employer/candidate: NO hard-delete endpoint exists (identity-cascade safety)</li>
 *   <li>validation failures → 400; missing resources → 404; duplicates → 409;
 *       unknown user linkage → 404; salary min > max → 400</li>
 *   <li>skill delete cascade; job category delete detaches jobs (SET NULL)</li>
 *   <li>candidate↔skill assign/duplicate/remove/404s</li>
 *   <li>pagination defaults + page-size cap; search/filter params</li>
 *   <li>DTO safety: no password hash / token / lockout internals in responses</li>
 * </ul>
 */
@WebMvcTest(controllers = {EmployerController.class, CandidateController.class,
        SkillController.class, CandidateSkillController.class, JobCategoryController.class})
@Import({SecurityConfig.class, CorsConfig.class, JwtKeyConfig.class,
        JwtTokenProvider.class, CustomUserDetailsService.class, SecurityUtils.class,
        EmployerService.class, CandidateService.class, SkillService.class,
        CandidateSkillService.class, JobCategoryService.class})
@AutoConfigureMockMvc
class AdminEmploymentSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EmployerRepository employerRepository;

    @MockitoBean
    private CandidateRepository candidateRepository;

    @MockitoBean
    private SkillRepository skillRepository;

    @MockitoBean
    private CandidateSkillRepository candidateSkillRepository;

    @MockitoBean
    private JobCategoryRepository jobCategoryRepository;

    @MockitoBean
    private UserRepository userRepository;

    private static final String EMPLOYERS = ApiPaths.ADMIN_EMPLOYERS;
    private static final String CANDIDATES = ApiPaths.ADMIN_CANDIDATES;
    private static final String SKILLS = ApiPaths.ADMIN_SKILLS;
    private static final String JOB_CATEGORIES = ApiPaths.ADMIN_JOB_CATEGORIES;

    private static final String SUPER_ADMIN_EMAIL = "superadmin@lokmitfoundation.org";
    private static final String ADMIN_EMAIL = "admin@lokmitfoundation.org";
    private static final String MODERATOR_EMAIL = "moderator@lokmitfoundation.org";
    private static final String EDITOR_EMAIL = "editor@lokmitfoundation.org";
    private static final String CANDIDATE_EMAIL = "candidate@lokmitfoundation.org";

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void resetStubs() {
        reset(employerRepository, candidateRepository, skillRepository,
                candidateSkillRepository, jobCategoryRepository, userRepository);
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

    private String tokenFor(long id, String email, List<String> claimRoles) {
        return jwtTokenProvider.generateAccessToken(id, email, claimRoles);
    }

    private User dbLinkedUser(long id) {
        User user = new User();
        user.setId(id);
        user.setEmail("linked-user-" + id + "@example.org");
        user.setFullName("Linked User");
        user.setUserType("EMPLOYER");
        user.setStatus("ACTIVE");
        user.setEmailVerified(true);
        user.setCreatedAt(OffsetDateTime.now());
        user.setUpdatedAt(OffsetDateTime.now());
        return user;
    }

    private Employer dbEmployer(long id, User user) {
        Employer e = new Employer();
        e.setId(id);
        e.setUser(user);
        e.setCompanyName("Acme Ltd");
        e.setVerificationStatus("UNVERIFIED");
        e.setStatus("ACTIVE");
        e.setCreatedAt(OffsetDateTime.now());
        e.setUpdatedAt(OffsetDateTime.now());
        return e;
    }

    private Candidate dbCandidate(long id, User user) {
        Candidate c = new Candidate();
        c.setId(id);
        c.setUser(user);
        c.setPhone("9841000000");
        c.setCurrentLocation("Kathmandu");
        c.setSummary("Java developer");
        c.setExpectedSalaryMin(new BigDecimal("1000.00"));
        c.setExpectedSalaryMax(new BigDecimal("2000.00"));
        c.setAvailabilityStatus("ACTIVELY_LOOKING");
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

    private JobCategory dbJobCategory(long id, String name, String slug) {
        JobCategory jc = new JobCategory();
        jc.setId(id);
        jc.setName(name);
        jc.setSlug(slug);
        jc.setDisplayOrder(0);
        jc.setStatus("ACTIVE");
        jc.setCreatedAt(OffsetDateTime.now());
        jc.setUpdatedAt(OffsetDateTime.now());
        return jc;
    }

    private CandidateSkill dbCandidateSkill(Candidate candidate, Skill skill) {
        CandidateSkill cs = new CandidateSkill();
        cs.setCandidate(candidate);
        cs.setSkill(skill);
        cs.setProficiency("ADVANCED");
        return cs;
    }

    // ------------------------------------------------------------------
    // A. Anonymous → 401 (probes on every endpoint family)
    // ------------------------------------------------------------------

    @Test
    @DisplayName("anonymous access is rejected with 401 on all employment namespaces")
    void anonymousIs401Everywhere() throws Exception {
        mockMvc.perform(get(EMPLOYERS)).andExpect(status().isUnauthorized());
        mockMvc.perform(get(EMPLOYERS + "/1")).andExpect(status().isUnauthorized());
        mockMvc.perform(post(EMPLOYERS).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":1,\"companyName\":\"C\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(patch(EMPLOYERS + "/1").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"SUSPENDED\"}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get(CANDIDATES)).andExpect(status().isUnauthorized());
        mockMvc.perform(get(CANDIDATES + "/1")).andExpect(status().isUnauthorized());
        mockMvc.perform(post(CANDIDATES).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":1}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(patch(CANDIDATES + "/1").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"123\"}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get(SKILLS)).andExpect(status().isUnauthorized());
        mockMvc.perform(get(SKILLS + "/1")).andExpect(status().isUnauthorized());
        mockMvc.perform(post(SKILLS).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Java\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(patch(SKILLS + "/1").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Java\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete(SKILLS + "/1")).andExpect(status().isUnauthorized());

        mockMvc.perform(get(CANDIDATES + "/1/skills")).andExpect(status().isUnauthorized());
        mockMvc.perform(post(CANDIDATES + "/1/skills").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"skillId\":2}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete(CANDIDATES + "/1/skills/2"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get(JOB_CATEGORIES)).andExpect(status().isUnauthorized());
        mockMvc.perform(get(JOB_CATEGORIES + "/1")).andExpect(status().isUnauthorized());
        mockMvc.perform(post(JOB_CATEGORIES).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Eng\",\"slug\":\"eng\",\"displayOrder\":0}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(patch(JOB_CATEGORIES + "/1").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Eng\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete(JOB_CATEGORIES + "/1")).andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------
    // B. Permission matrix
    // ------------------------------------------------------------------

    @Test
    @DisplayName("MODERATOR and EDITOR (no employment:manage) get 403 on employer/skill/job-category endpoints")
    void moderatorAndEditorGet403OnEmploymentEndpoints() throws Exception {
        Role moderator = role("MODERATOR", "jobs:moderate", "messages:manage");
        givenDbUser(dbUser(3L, MODERATOR_EMAIL, moderator));
        String moderatorToken = tokenFor(3L, MODERATOR_EMAIL, List.of("MODERATOR"));

        mockMvc.perform(get(EMPLOYERS).header("Authorization", "Bearer " + moderatorToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(SKILLS).header("Authorization", "Bearer " + moderatorToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(SKILLS).header("Authorization", "Bearer " + moderatorToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"X\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete(SKILLS + "/1").header("Authorization", "Bearer " + moderatorToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(JOB_CATEGORIES).header("Authorization", "Bearer " + moderatorToken))
                .andExpect(status().isForbidden());

        Role editor = role("EDITOR", "content:manage", "downloads:manage");
        givenDbUser(dbUser(4L, EDITOR_EMAIL, editor));
        String editorToken = tokenFor(4L, EDITOR_EMAIL, List.of("EDITOR"));

        mockMvc.perform(get(EMPLOYERS).header("Authorization", "Bearer " + editorToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete(JOB_CATEGORIES + "/1").header("Authorization", "Bearer " + editorToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("users with candidates:manage but not employment:manage get 403 on employer endpoints")
    void candidatesOnlyPermissionCannotAccessEmployers() throws Exception {
        // A hypothetical DB state where a role has candidates:manage only.
        Role candidatesOnly = role("CANDIDATES_ONLY", "candidates:manage");
        givenDbUser(dbUser(9L, "candonly@lokmitfoundation.org", candidatesOnly));
        String token = tokenFor(9L, "candonly@lokmitfoundation.org", List.of("CANDIDATES_ONLY"));

        mockMvc.perform(get(EMPLOYERS).header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(SKILLS).header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("normal platform user (CANDIDATE role) gets 403 everywhere")
    void candidateRoleGets403() throws Exception {
        Role candidate = role("CANDIDATE");
        givenDbUser(dbUser(5L, CANDIDATE_EMAIL, candidate));
        String token = tokenFor(5L, CANDIDATE_EMAIL, List.of("CANDIDATE"));

        mockMvc.perform(get(EMPLOYERS).header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(CANDIDATES).header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("spoofed JWT role claims do not elevate privileges")
    void spoofedJwtRoleClaimsDoNotElevate() throws Exception {
        // DB says the user is a plain CANDIDATE; the token claims SUPER_ADMIN.
        Role candidate = role("CANDIDATE");
        givenDbUser(dbUser(6L, "spoof@lokmitfoundation.org", candidate));
        String spoofed = tokenFor(6L, "spoof@lokmitfoundation.org",
                List.of("SUPER_ADMIN", "ADMIN"));

        mockMvc.perform(get(EMPLOYERS).header("Authorization", "Bearer " + spoofed))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(EMPLOYERS).header("Authorization", "Bearer " + spoofed)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":1,\"companyName\":\"C\"}"))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------
    // C. ADMIN/SUPER_ADMIN success paths
    // ------------------------------------------------------------------

    @Test
    @DisplayName("ADMIN can list, get, create and update employers; NO delete endpoint exists")
    void adminEmployerCrud() throws Exception {
        Role admin = role("ADMIN", "dashboard:view", "content:manage", "content:publish",
                "downloads:manage", "messages:manage", "jobs:manage", "settings:manage",
                "employment:manage", "candidates:manage");
        givenDbUser(dbUser(2L, ADMIN_EMAIL, admin));
        String token = tokenFor(2L, ADMIN_EMAIL, List.of("ADMIN"));
        String auth = "Bearer " + token;

        // list (empty)
        when(employerRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));
        mockMvc.perform(get(EMPLOYERS).header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.totalItems").value(0));

        // get existing
        User linked = dbLinkedUser(42L);
        Employer employer = dbEmployer(1L, linked);
        when(employerRepository.findById(1L)).thenReturn(Optional.of(employer));
        mockMvc.perform(get(EMPLOYERS + "/1").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.companyName").value("Acme Ltd"))
                .andExpect(jsonPath("$.data.userId").value(42));

        // get missing → 404
        when(employerRepository.findById(99L)).thenReturn(Optional.empty());
        mockMvc.perform(get(EMPLOYERS + "/99").header("Authorization", auth))
                .andExpect(status().isNotFound());

        // create
        when(userRepository.findById(42L)).thenReturn(Optional.of(linked));
        when(employerRepository.existsByUserId(42L)).thenReturn(false);
        when(employerRepository.save(any(Employer.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        mockMvc.perform(post(EMPLOYERS).header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":42,"companyName":"Acme Ltd","websiteUrl":"https://acme.example","verificationStatus":"PENDING","status":"ACTIVE"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.companyName").value("Acme Ltd"))
                .andExpect(jsonPath("$.data.verificationStatus").value("PENDING"));

        // create with unknown user → 404
        when(userRepository.findById(777L)).thenReturn(Optional.empty());
        mockMvc.perform(post(EMPLOYERS).header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":777,\"companyName\":\"Ghost Co\"}"))
                .andExpect(status().isNotFound());

        // create duplicate user linkage → 409
        when(employerRepository.existsByUserId(42L)).thenReturn(true);
        mockMvc.perform(post(EMPLOYERS).header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":42,\"companyName\":\"Second Co\"}"))
                .andExpect(status().isConflict());

        // update
        when(employerRepository.save(any(Employer.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        mockMvc.perform(patch(EMPLOYERS + "/1").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"SUSPENDED\",\"verificationStatus\":\"VERIFIED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUSPENDED"))
                .andExpect(jsonPath("$.data.verificationStatus").value("VERIFIED"));

        // NO delete endpoint (identity-cascade safety): 405 from Spring MVC
        mockMvc.perform(delete(EMPLOYERS + "/1").header("Authorization", auth))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    @DisplayName("SUPER_ADMIN can create and update candidates; salary validation and NO delete endpoint")
    void superAdminCandidateCrud() throws Exception {
        Role superAdmin = role("SUPER_ADMIN", "users:manage", "dashboard:view",
                "content:manage", "content:publish", "downloads:manage", "messages:manage",
                "jobs:manage", "jobs:moderate", "settings:manage", "employment:manage",
                "candidates:manage");
        givenDbUser(dbUser(1L, SUPER_ADMIN_EMAIL, superAdmin));
        String token = tokenFor(1L, SUPER_ADMIN_EMAIL, List.of("SUPER_ADMIN"));
        String auth = "Bearer " + token;

        // list with search + filter
        when(candidateRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(dbCandidate(1L, dbLinkedUser(42L)),
                        dbCandidate(2L, dbLinkedUser(43L))), PageRequest.of(0, 20), 2));
        mockMvc.perform(get(CANDIDATES).header("Authorization", auth)
                        .param("search", "kathmandu").param("availability", "ACTIVELY_LOOKING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalItems").value(2));

        // create
        User linked = dbLinkedUser(42L);
        when(userRepository.findById(42L)).thenReturn(Optional.of(linked));
        when(candidateRepository.existsByUserId(42L)).thenReturn(false);
        when(candidateRepository.save(any(Candidate.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        mockMvc.perform(post(CANDIDATES).header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":42,"phone":"9841000000","currentLocation":"Kathmandu","expectedSalaryMin":1000.00,"expectedSalaryMax":2000.00,"availability":"OPEN_TO_OFFERS"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.availability").value("OPEN_TO_OFFERS"));

        // create with min > max → 400
        mockMvc.perform(post(CANDIDATES).header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":42,"expectedSalaryMin":3000.00,"expectedSalaryMax":2000.00}
                                """))
                .andExpect(status().isBadRequest());

        // create duplicate user linkage → 409
        when(candidateRepository.existsByUserId(42L)).thenReturn(true);
        mockMvc.perform(post(CANDIDATES).header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":42,\"phone\":\"1\"}"))
                .andExpect(status().isConflict());

        // update
        Candidate existing = dbCandidate(1L, linked);
        when(candidateRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(candidateRepository.save(any(Candidate.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        mockMvc.perform(patch(CANDIDATES + "/1").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"availability\":\"NOT_LOOKING\",\"currentLocation\":\"Pokhara\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.availability").value("NOT_LOOKING"));

        // unknown candidate → 404
        when(candidateRepository.findById(99L)).thenReturn(Optional.empty());
        mockMvc.perform(patch(CANDIDATES + "/99").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"123\"}"))
                .andExpect(status().isNotFound());

        // NO delete endpoint (identity-cascade safety)
        mockMvc.perform(delete(CANDIDATES + "/1").header("Authorization", auth))
                .andExpect(status().isMethodNotAllowed());
    }

    // ------------------------------------------------------------------
    // D. Validation → 400
    // ------------------------------------------------------------------

    @Test
    @DisplayName("validation failures return 400 across employment namespaces")
    void validationFailuresAre400() throws Exception {
        Role admin = role("ADMIN", "employment:manage", "candidates:manage");
        givenDbUser(dbUser(2L, ADMIN_EMAIL, admin));
        String auth = "Bearer " + tokenFor(2L, ADMIN_EMAIL, List.of("ADMIN"));

        // employer: missing userId + blank companyName
        mockMvc.perform(post(EMPLOYERS).header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyName\":\"\"}"))
                .andExpect(status().isBadRequest());

        // employer: companyName over 255 chars
        mockMvc.perform(post(EMPLOYERS).header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":1,\"companyName\":\"" + "x".repeat(256) + "\"}"))
                .andExpect(status().isBadRequest());

        // employer: invalid status value
        mockMvc.perform(patch(EMPLOYERS + "/1").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DELETED\"}"))
                .andExpect(status().isBadRequest());

        // candidate: invalid availability
        mockMvc.perform(post(CANDIDATES).header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":1,\"availability\":\"DELETED\"}"))
                .andExpect(status().isBadRequest());

        // candidate: invalid gender
        mockMvc.perform(post(CANDIDATES).header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":1,\"gender\":\"X\"}"))
                .andExpect(status().isBadRequest());

        // skill: blank name
        mockMvc.perform(post(SKILLS).header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest());

        // skill: name over 100 chars
        mockMvc.perform(post(SKILLS).header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + "x".repeat(101) + "\"}"))
                .andExpect(status().isBadRequest());

        // job category: bad slug format
        mockMvc.perform(post(JOB_CATEGORIES).header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Eng\",\"slug\":\"Bad Slug!\",\"displayOrder\":0}"))
                .andExpect(status().isBadRequest());

        // job category: missing displayOrder
        mockMvc.perform(post(JOB_CATEGORIES).header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Eng\",\"slug\":\"eng\"}"))
                .andExpect(status().isBadRequest());

        // candidate skill: invalid proficiency
        mockMvc.perform(post(CANDIDATES + "/1/skills").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"skillId\":2,\"proficiency\":\"MASTER\"}"))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------
    // E. Skills CRUD, duplicates and cascade delete
    // ------------------------------------------------------------------

    @Test
    @DisplayName("skills CRUD works; duplicate name → 409; delete cascades assignments")
    void skillsCrudAndCascade() throws Exception {
        Role admin = role("ADMIN", "employment:manage", "candidates:manage");
        givenDbUser(dbUser(2L, ADMIN_EMAIL, admin));
        String auth = "Bearer " + tokenFor(2L, ADMIN_EMAIL, List.of("ADMIN"));

        // list + search
        when(skillRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(dbSkill(1L, "Java")), PageRequest.of(0, 20), 1));
        mockMvc.perform(get(SKILLS).header("Authorization", auth).param("search", "jav"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].name").value("Java"));

        // get existing / missing
        when(skillRepository.findById(1L)).thenReturn(Optional.of(dbSkill(1L, "Java")));
        mockMvc.perform(get(SKILLS + "/1").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Java"));
        when(skillRepository.findById(99L)).thenReturn(Optional.empty());
        mockMvc.perform(get(SKILLS + "/99").header("Authorization", auth))
                .andExpect(status().isNotFound());

        // create
        when(skillRepository.existsByNameIgnoreCase("Python")).thenReturn(false);
        when(skillRepository.save(any(Skill.class))).thenAnswer(inv -> inv.getArgument(0));
        mockMvc.perform(post(SKILLS).header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Python\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("Python"));

        // duplicate create → 409
        when(skillRepository.existsByNameIgnoreCase("Java")).thenReturn(true);
        mockMvc.perform(post(SKILLS).header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Java\"}"))
                .andExpect(status().isConflict());

        // update (rename conflict → 409)
        Skill java = dbSkill(1L, "Java");
        when(skillRepository.findById(1L)).thenReturn(Optional.of(java));
        when(skillRepository.existsByNameIgnoreCase("Python")).thenReturn(true);
        mockMvc.perform(patch(SKILLS + "/1").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Python\"}"))
                .andExpect(status().isConflict());

        // successful update
        when(skillRepository.existsByNameIgnoreCase("Java EE")).thenReturn(false);
        mockMvc.perform(patch(SKILLS + "/1").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Java EE\",\"status\":\"INACTIVE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("INACTIVE"));

        // delete existing → 204 (cascades per V8)
        when(skillRepository.existsById(1L)).thenReturn(true);
        mockMvc.perform(delete(SKILLS + "/1").header("Authorization", auth))
                .andExpect(status().isNoContent());
        verify(skillRepository).deleteById(1L);

        // delete missing → 404
        when(skillRepository.existsById(99L)).thenReturn(false);
        mockMvc.perform(delete(SKILLS + "/99").header("Authorization", auth))
                .andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------
    // F. Candidate ↔ skills
    // ------------------------------------------------------------------

    @Test
    @DisplayName("candidate skill assignment: list, assign, duplicate 409, unknown 404, remove")
    void candidateSkillAssignments() throws Exception {
        Role admin = role("ADMIN", "candidates:manage");
        givenDbUser(dbUser(2L, ADMIN_EMAIL, admin));
        String auth = "Bearer " + tokenFor(2L, ADMIN_EMAIL, List.of("ADMIN"));

        Candidate candidate = dbCandidate(1L, dbLinkedUser(42L));
        Skill java = dbSkill(1L, "Java");

        // unknown candidate → 404
        when(candidateRepository.existsById(77L)).thenReturn(false);
        mockMvc.perform(get(CANDIDATES + "/77/skills").header("Authorization", auth))
                .andExpect(status().isNotFound());
        mockMvc.perform(post(CANDIDATES + "/77/skills").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"skillId\":1}"))
                .andExpect(status().isNotFound());

        // list assignments
        when(candidateRepository.existsById(1L)).thenReturn(true);
        when(candidateSkillRepository.findByCandidateId(1L))
                .thenReturn(List.of(dbCandidateSkill(candidate, java)));
        mockMvc.perform(get(CANDIDATES + "/1/skills").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].skillName").value("Java"))
                .andExpect(jsonPath("$.data[0].proficiency").value("ADVANCED"));

        // assign
        when(skillRepository.findById(1L)).thenReturn(Optional.of(java));
        when(candidateRepository.getReferenceById(1L)).thenReturn(candidate);
        when(candidateSkillRepository.existsByCandidateIdAndSkillId(1L, 1L)).thenReturn(false);
        when(candidateSkillRepository.save(any(CandidateSkill.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        mockMvc.perform(post(CANDIDATES + "/1/skills").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"skillId\":1,\"proficiency\":\"ADVANCED\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.skillId").value(1))
                .andExpect(jsonPath("$.data.proficiency").value("ADVANCED"));

        // duplicate assignment → 409
        when(candidateSkillRepository.existsByCandidateIdAndSkillId(1L, 1L)).thenReturn(true);
        mockMvc.perform(post(CANDIDATES + "/1/skills").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"skillId\":1}"))
                .andExpect(status().isConflict());

        // unknown skill → 404
        when(skillRepository.findById(99L)).thenReturn(Optional.empty());
        mockMvc.perform(post(CANDIDATES + "/1/skills").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"skillId\":99}"))
                .andExpect(status().isNotFound());

        // remove assignment → 204
        when(candidateSkillRepository.existsById(any())).thenReturn(true);
        mockMvc.perform(delete(CANDIDATES + "/1/skills/1").header("Authorization", auth))
                .andExpect(status().isNoContent());

        // remove nonexistent → 404
        when(candidateSkillRepository.existsById(any())).thenReturn(false);
        mockMvc.perform(delete(CANDIDATES + "/1/skills/999").header("Authorization", auth))
                .andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------
    // G. Job categories
    // ------------------------------------------------------------------

    @Test
    @DisplayName("job categories CRUD; duplicate name/slug 409; delete detaches jobs via SET NULL")
    void jobCategoriesCrud() throws Exception {
        Role admin = role("ADMIN", "employment:manage");
        givenDbUser(dbUser(2L, ADMIN_EMAIL, admin));
        String auth = "Bearer " + tokenFor(2L, ADMIN_EMAIL, List.of("ADMIN"));

        // list ordered
        when(jobCategoryRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(dbJobCategory(1L, "Engineering", "engineering")),
                        PageRequest.of(0, 20), 1));
        mockMvc.perform(get(JOB_CATEGORIES).header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].slug").value("engineering"));

        // create
        when(jobCategoryRepository.existsByNameIgnoreCase("Health")).thenReturn(false);
        when(jobCategoryRepository.existsBySlugIgnoreCase("health")).thenReturn(false);
        when(jobCategoryRepository.save(any(JobCategory.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        mockMvc.perform(post(JOB_CATEGORIES).header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Health\",\"slug\":\"health\",\"displayOrder\":2,\"description\":\"Health jobs\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.slug").value("health"));

        // duplicate name → 409
        when(jobCategoryRepository.existsByNameIgnoreCase("Engineering")).thenReturn(true);
        mockMvc.perform(post(JOB_CATEGORIES).header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Engineering\",\"slug\":\"engineering-2\",\"displayOrder\":0}"))
                .andExpect(status().isConflict());

        // duplicate slug → 409
        when(jobCategoryRepository.existsByNameIgnoreCase("Education")).thenReturn(false);
        when(jobCategoryRepository.existsBySlugIgnoreCase("engineering")).thenReturn(true);
        mockMvc.perform(post(JOB_CATEGORIES).header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Education\",\"slug\":\"engineering\",\"displayOrder\":0}"))
                .andExpect(status().isConflict());

        // update (slug immutable — no slug field in DTO; name conflict → 409)
        JobCategory engineering = dbJobCategory(1L, "Engineering", "engineering");
        when(jobCategoryRepository.findById(1L)).thenReturn(Optional.of(engineering));
        when(jobCategoryRepository.existsByNameIgnoreCaseAndIdNot("Health", 1L)).thenReturn(true);
        mockMvc.perform(patch(JOB_CATEGORIES + "/1").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Health\"}"))
                .andExpect(status().isConflict());

        // successful update
        when(jobCategoryRepository.existsByNameIgnoreCaseAndIdNot("Healthcare", 1L))
                .thenReturn(false);
        mockMvc.perform(patch(JOB_CATEGORIES + "/1").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Healthcare\",\"displayOrder\":5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.displayOrder").value(5))
                .andExpect(jsonPath("$.data.slug").value("engineering"));

        // delete → 204 (jobs detached via FK SET NULL — repository delete only)
        when(jobCategoryRepository.existsById(1L)).thenReturn(true);
        mockMvc.perform(delete(JOB_CATEGORIES + "/1").header("Authorization", auth))
                .andExpect(status().isNoContent());
        verify(jobCategoryRepository).deleteById(1L);

        // delete missing → 404
        when(jobCategoryRepository.existsById(99L)).thenReturn(false);
        mockMvc.perform(delete(JOB_CATEGORIES + "/99").header("Authorization", auth))
                .andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------
    // H. Pagination
    // ------------------------------------------------------------------

    @Test
    @DisplayName("pagination defaults and page-size cap behave as project convention")
    void paginationBehavior() throws Exception {
        Role admin = role("ADMIN", "employment:manage");
        givenDbUser(dbUser(2L, ADMIN_EMAIL, admin));
        String auth = "Bearer " + tokenFor(2L, ADMIN_EMAIL, List.of("ADMIN"));

        // default page size 20
        when(skillRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenAnswer(inv -> new PageImpl<>(List.of(), inv.getArgument(1,
                        Pageable.class), 0));
        mockMvc.perform(get(SKILLS).header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.page").value(0));

        // cap: size=200 → 400
        mockMvc.perform(get(SKILLS).header("Authorization", auth)
                        .param("size", "200"))
                .andExpect(status().isBadRequest());

        // negative page → 400
        mockMvc.perform(get(SKILLS).header("Authorization", auth)
                        .param("page", "-1"))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------
    // I. DTO safety
    // ------------------------------------------------------------------

    @Test
    @DisplayName("employer/candidate responses never expose security-sensitive user fields")
    void dtoSafety() throws Exception {
        Role admin = role("ADMIN", "employment:manage", "candidates:manage");
        givenDbUser(dbUser(2L, ADMIN_EMAIL, admin));
        String auth = "Bearer " + tokenFor(2L, ADMIN_EMAIL, List.of("ADMIN"));

        User linked = dbLinkedUser(42L);
        // give the linked user real security internals to prove they are not serialized
        linked.setPasswordHash("$2a$10$secret-hash-should-never-leak");
        linked.setFailedLoginAttempts(3);
        linked.setLockedUntil(OffsetDateTime.now().plusMinutes(10));

        when(employerRepository.findById(1L)).thenReturn(Optional.of(dbEmployer(1L, linked)));
        when(candidateRepository.findById(1L)).thenReturn(Optional.of(dbCandidate(1L, linked)));

        String employerBody = mockMvc.perform(get(EMPLOYERS + "/1").header("Authorization", auth))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(employerBody)
                .doesNotContain("password")
                .doesNotContain("secret-hash")
                .doesNotContain("failedLogin")
                .doesNotContain("lockedUntil")
                .doesNotContain("refreshToken")
                .doesNotContain("userType")
                .doesNotContain("email")
                .doesNotContain("\"roles\"");

        String candidateBody = mockMvc.perform(get(CANDIDATES + "/1").header("Authorization", auth))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(candidateBody)
                .doesNotContain("password")
                .doesNotContain("secret-hash")
                .doesNotContain("failedLogin")
                .doesNotContain("lockedUntil")
                .doesNotContain("refreshToken")
                .doesNotContain("\"roles\"");
    }
}
