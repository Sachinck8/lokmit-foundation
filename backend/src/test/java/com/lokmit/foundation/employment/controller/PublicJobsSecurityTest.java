package com.lokmit.foundation.employment.controller;

import com.lokmit.foundation.common.constants.ApiPaths;
import com.lokmit.foundation.employment.candidate.repository.CandidateRepository;
import com.lokmit.foundation.employment.employer.entity.Employer;
import com.lokmit.foundation.employment.employer.repository.EmployerRepository;
import com.lokmit.foundation.employment.job.controller.JobController;
import com.lokmit.foundation.employment.job.controller.PublicJobController;
import com.lokmit.foundation.employment.job.entity.Job;
import com.lokmit.foundation.employment.job.entity.JobSkill;
import com.lokmit.foundation.employment.job.repository.JobRepository;
import com.lokmit.foundation.employment.job.repository.JobSkillRepository;
import com.lokmit.foundation.employment.job.service.JobService;
import com.lokmit.foundation.employment.job.service.JobSkillService;
import com.lokmit.foundation.employment.job.service.PublicJobService;
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
import org.mockito.Mockito;
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
 * A8 security and behavior matrix exercised through the REAL Spring Security
 * filter chain (identical setup to the A7.2 admin jobs matrix).
 *
 * <p>Coverage: anonymous public list + detail succeed; only PUBLISHED jobs
 * are ever visible (drafts/closed/archived are invisible, detail 404 without
 * existence leak); unknown id 404; pagination defaults/cap; filters/search;
 * public DTO safety (no status/updatedAt/verificationStatus/employer user
 * internals); and the A8 write-protection guarantee — POST/PATCH/PUT/DELETE
 * on the public path stay 401, while the admin job CRUD + lifecycle remain
 * permission-guarded exactly as in A7.2.</p>
 */
@WebMvcTest(controllers = {PublicJobController.class, JobController.class})
@Import({SecurityConfig.class, CorsConfig.class, JwtKeyConfig.class,
        JwtTokenProvider.class, CustomUserDetailsService.class, SecurityUtils.class,
        JobService.class, JobSkillService.class, PublicJobService.class})
@AutoConfigureMockMvc
class PublicJobsSecurityTest {

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

    private static final String JOBS = ApiPaths.JOBS;
    private static final String ADMIN_JOBS = ApiPaths.ADMIN_JOBS;

    private static final String ADMIN_EMAIL = "admin@lokmitfoundation.org";
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
    // fixtures
    // ------------------------------------------------------------------

    private User dbUser(long id, String email, Role... roles) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setPasswordHash("hash-not-used-by-jwt-filter-0123456789");
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
        role.setPermissions(Set.of(permissionCodes).stream().map(pc -> {
            Permission p = new Permission();
            p.setCode(pc);
            return p;
        }).collect(Collectors.toSet()));
        return role;
    }

    private Employer employer(long id, String companyName) {
        Employer e = new Employer();
        e.setId(id);
        e.setCompanyName(companyName);
        e.setVerificationStatus("APPROVED");
        e.setCreatedAt(OffsetDateTime.now());
        e.setUpdatedAt(OffsetDateTime.now());
        return e;
    }

    private Job job(long id, String status, String title) {
        Job j = new Job();
        j.setId(id);
        j.setEmployer(employer(500L + id, "Hope Works " + id));
        j.setSlug("published-role-" + id);
        j.setTitle(title);
        j.setDescription("Public description for job " + id);
        j.setRequirements("Public requirements for job " + id);
        j.setEmploymentType(Job.EMPLOYMENT_FULL_TIME);
        j.setWorkMode(Job.WORK_MODE_HYBRID);
        j.setWorkLocation("Patna, Bihar");
        j.setSalaryMin(new BigDecimal("30000.00"));
        j.setSalaryMax(new BigDecimal("45000.00"));
        j.setSalaryCurrency("INR");
        j.setOpenings(2);
        j.setStatus(status);
        j.setApplicationDeadline(LocalDate.of(2026, 12, 31));
        j.setPublishedAt(Job.STATUS_PUBLISHED.equals(status)
                ? OffsetDateTime.parse("2026-09-01T10:15:30Z") : null);
        j.setCreatedAt(OffsetDateTime.now());
        j.setUpdatedAt(OffsetDateTime.now());
        return j;
    }

    private void stubSkills(long jobId, long skillId, String skillName) {
        Skill skill = new Skill();
        skill.setId(skillId);
        skill.setName(skillName);
        skill.setStatus("ACTIVE");
        JobSkill js = new JobSkill();
        js.setJob(new Job()); // only ids are read by the mapper
        js.getJob().setId(jobId);
        js.setSkill(skill);
        when(jobSkillRepository.findByJobId(jobId)).thenReturn(List.of(js));
    }

    private String tokenFor(long userId, String email) {
        return jwtTokenProvider.generateAccessToken(userId, email, List.of());
    }

    // ------------------------------------------------------------------
    // anonymous access — the A8 core guarantee
    // ------------------------------------------------------------------

    @Test
    @DisplayName("anonymous list of published jobs succeeds without authentication")
    void anonymousListSucceeds() throws Exception {
        Job published = job(1L, Job.STATUS_PUBLISHED, "Field Coordinator");
        when(jobRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(published), PageRequest.of(0, 20), 1));
        stubSkills(1L, 61L, "Community Outreach");

        mockMvc.perform(get(JOBS).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].id").value(1))
                .andExpect(jsonPath("$.data.items[0].title").value("Field Coordinator"))
                .andExpect(jsonPath("$.data.items[0].companyName").value("Hope Works 1"))
                .andExpect(jsonPath("$.data.items[0].skills[0].name").value("Community Outreach"));
    }

    @Test
    @DisplayName("anonymous detail of a published job succeeds without authentication")
    void anonymousDetailSucceeds() throws Exception {
        when(jobRepository.findById(1L))
                .thenReturn(Optional.of(job(1L, Job.STATUS_PUBLISHED, "Field Coordinator")));
        stubSkills(1L, 61L, "Community Outreach");

        mockMvc.perform(get(JOBS + "/1").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.slug").value("published-role-1"))
                .andExpect(jsonPath("$.data.employmentType").value("FULL_TIME"))
                .andExpect(jsonPath("$.data.workMode").value("HYBRID"))
                .andExpect(jsonPath("$.data.salaryCurrency").value("INR"));
    }

    // ------------------------------------------------------------------
    // visibility rules — only PUBLISHED is public
    // ------------------------------------------------------------------

    @Test
    @DisplayName("detail: DRAFT, CLOSED and ARCHIVED jobs return the same 404 as unknown ids")
    void nonPublishedDetailIs404WithoutExistenceLeak() throws Exception {
        for (long id : new long[]{11L, 12L, 13L}) {
            when(jobRepository.findById(id)).thenReturn(Optional.empty());
        }
        Job draft = job(11L, Job.STATUS_DRAFT, "Hidden Draft");
        Job closed = job(12L, Job.STATUS_CLOSED, "Hidden Closed");
        Job archived = job(13L, Job.STATUS_ARCHIVED, "Hidden Archived");
        when(jobRepository.findById(11L)).thenReturn(Optional.of(draft));
        when(jobRepository.findById(12L)).thenReturn(Optional.of(closed));
        when(jobRepository.findById(13L)).thenReturn(Optional.of(archived));

        for (long id : new long[]{11L, 12L, 13L, 99L}) {
            mockMvc.perform(get(JOBS + "/" + id))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false));
        }
        // Skills are never queried for non-public jobs — no leak through side effects.
        verify(jobSkillRepository, never()).findByJobId(anyLong());
    }

    @Test
    @DisplayName("unknown job detail returns 404 with the standard error envelope")
    void unknownJobDetailIs404() throws Exception {
        when(jobRepository.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get(JOBS + "/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errors[0].code").value("NOT_FOUND"));
    }

    // ------------------------------------------------------------------
    // pagination + filters
    // ------------------------------------------------------------------

    @Test
    @DisplayName("list honors page/size and exposes PageResponse metadata")
    void listPaginationWorks() throws Exception {
        Job first = job(2L, Job.STATUS_PUBLISHED, "Job A");
        Job second = job(3L, Job.STATUS_PUBLISHED, "Job B");
        when(jobRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(first, second), PageRequest.of(1, 2), 5));
        stubSkills(2L, 62L, "Skill A");
        stubSkills(3L, 63L, "Skill B");

        mockMvc.perform(get(JOBS).param("page", "1").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(2))
                .andExpect(jsonPath("$.data.totalItems").value(5))
                .andExpect(jsonPath("$.data.totalPages").value(3))
                .andExpect(jsonPath("$.data.items.length()").value(2));
    }

    @Test
    @DisplayName("list size above the cap is rejected (400), matching PageParams validation")
    void oversizedPageIsRejected() throws Exception {
        mockMvc.perform(get(JOBS).param("size", "101"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("supported filters/search bind and pass through to the repository")
    void filtersBindAndPassThrough() throws Exception {
        when(jobRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get(JOBS)
                        .param("categoryId", "7")
                        .param("employmentType", "FULL_TIME")
                        .param("workMode", "REMOTE")
                        .param("search", "coordinator"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isEmpty());

        org.mockito.ArgumentCaptor<Pageable> pageableCaptor =
                org.mockito.ArgumentCaptor.forClass(Pageable.class);
        verify(jobRepository).findAll(any(Specification.class), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getSort().getOrderFor("publishedAt"))
                .isNotNull();
    }

    // ------------------------------------------------------------------
    // DTO safety — no internals leak through the public surface
    // ------------------------------------------------------------------

    @Test
    @DisplayName("public DTO never exposes status, updatedAt, verificationStatus or admin fields")
    void publicDtoDoesNotLeakInternals() throws Exception {
        Job published = job(1L, Job.STATUS_PUBLISHED, "Field Coordinator");
        when(jobRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(published), PageRequest.of(0, 20), 1));
        stubSkills(1L, 61L, "Community Outreach");

        String body = mockMvc.perform(get(JOBS))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(body)
                .doesNotContain("\"status\"")
                .doesNotContain("\"updatedAt\"")
                .doesNotContain("\"createdAt\"")
                .doesNotContain("\"verificationStatus\"")
                .doesNotContain("APPROVED")
                .doesNotContain("\"employer\"")
                .doesNotContain("\"user\"");
    }

    // ------------------------------------------------------------------
    // A8 write protection — the public path must never gain mutations
    // ------------------------------------------------------------------

    @Test
    @DisplayName("POST/PATCH/PUT/DELETE on the public /jobs path are NOT public (401)")
    void publicPathMutationsAreNotPublic() throws Exception {
        String body = "{}";

        mockMvc.perform(post(JOBS).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(patch(JOBS + "/1").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put(JOBS + "/1").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete(JOBS + "/1"))
                .andExpect(status().isUnauthorized());

        // And no mutation ever reached the repositories through the public path.
        verify(jobRepository, never()).save(any(Job.class));
        verify(jobRepository, never()).deleteById(anyLong());
    }

    // ------------------------------------------------------------------
    // existing admin job management remains exactly as protected as before
    // ------------------------------------------------------------------

    @Test
    @DisplayName("admin job list stays permission-guarded: anonymous 401, candidate-role 403, admin 200")
    void adminJobEndpointsRemainProtected() throws Exception {
        // Anonymous → 401 (unchanged from A7.2).
        mockMvc.perform(get(ADMIN_JOBS))
                .andExpect(status().isUnauthorized());

        // CANDIDATE role (no employment:manage) → 403.
        when(userRepository.findByEmail(CANDIDATE_EMAIL)).thenReturn(Optional.of(
                dbUser(2L, CANDIDATE_EMAIL, role("CANDIDATE"))));
        mockMvc.perform(get(ADMIN_JOBS)
                        .header("Authorization", "Bearer " + tokenFor(2L, CANDIDATE_EMAIL)))
                .andExpect(status().isForbidden());

        // ADMIN with employment:manage → 200 (existing behavior preserved).
        when(userRepository.findByEmail(ADMIN_EMAIL)).thenReturn(Optional.of(
                dbUser(4L, ADMIN_EMAIL, role("ADMIN", "employment:manage"))));
        when(jobRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));
        mockMvc.perform(get(ADMIN_JOBS)
                        .header("Authorization", "Bearer " + tokenFor(4L, ADMIN_EMAIL)))
                .andExpect(status().isOk());

        // Anonymous admin-detail stays 401 (never swept into the public matchers).
        mockMvc.perform(get(ADMIN_JOBS + "/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("admin job mutations (create/update/delete/lifecycle) remain anonymous-401")
    void adminJobMutationsRemainProtected() throws Exception {
        String body = "{}";
        mockMvc.perform(post(ADMIN_JOBS).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(patch(ADMIN_JOBS + "/1").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete(ADMIN_JOBS + "/1"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post(ADMIN_JOBS + "/1/publish"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post(ADMIN_JOBS + "/1/close"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post(ADMIN_JOBS + "/1/archive"))
                .andExpect(status().isUnauthorized());
    }
}
