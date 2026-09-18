package com.lokmit.foundation.employment.resume.controller;

import com.lokmit.foundation.common.constants.ApiPaths;
import com.lokmit.foundation.employment.candidate.entity.Candidate;
import com.lokmit.foundation.employment.candidate.repository.CandidateRepository;
import com.lokmit.foundation.employment.resume.entity.Resume;
import com.lokmit.foundation.employment.resume.repository.ResumeRepository;
import com.lokmit.foundation.employment.resume.service.ResumeDeleteService;
import com.lokmit.foundation.employment.resume.service.ResumeDownloadService;
import com.lokmit.foundation.employment.resume.service.ResumeOwnershipService;
import com.lokmit.foundation.employment.resume.service.storage.FileStorage;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A7.6.5 API security matrix for DELETE /api/v1/resumes/{resumeId},
 * through the REAL Spring Security filter chain (real JWT filter, real
 * token provider, repository-stubbed users — no database). The REAL
 * {@link ResumeDeleteService} runs with persistence mocked at the
 * repository/FileStorage boundary, so authorization, active-rule and
 * cleanup-ordering decisions are exercised end-to-end.
 */
@WebMvcTest(controllers = ResumeDownloadController.class)
@Import({SecurityConfig.class, CorsConfig.class, JwtKeyConfig.class,
        JwtTokenProvider.class, CustomUserDetailsService.class, SecurityUtils.class,
        ResumeDeleteService.class, ResumeDownloadService.class, ResumeOwnershipService.class})
@AutoConfigureMockMvc
class ResumeDeleteApiSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private CandidateRepository candidateRepository;

    @MockitoBean
    private ResumeRepository resumeRepository;

    @MockitoBean
    private FileStorage fileStorage;

    private static final String ENDPOINT = ApiPaths.RESUMES;

    // ------------------------------------------------------------------
    // Fixtures
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

    private Resume resume(long id, long candidateId, boolean active) {
        Resume r = new Resume();
        r.setId(id);
        r.setCandidateId(candidateId);
        r.setFileName("cv.pdf");
        r.setFileType("application/pdf");
        r.setFileSizeBytes(100L);
        r.setActive(active);
        r.setCreatedAt(OffsetDateTime.now());
        r.setChecksumSha256("e".repeat(64));
        r.setStorageKey("resumes/" + id + "/test-key");
        return r;
    }

    private void givenCandidate(long userId, String email, long candidateId, Role... roles) {
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(dbUser(userId, email, roles)));
        Candidate c = new Candidate();
        c.setId(candidateId);
        c.setAvailabilityStatus(Candidate.AVAILABILITY_ACTIVELY_LOOKING);
        c.setCreatedAt(OffsetDateTime.now());
        c.setUpdatedAt(OffsetDateTime.now());
        when(candidateRepository.findByUserId(userId)).thenReturn(Optional.of(c));
    }

    private String tokenFor(long userId, String email) {
        return jwtTokenProvider.generateAccessToken(userId, email, List.of());
    }

    // ------------------------------------------------------------------
    // Authentication
    // ------------------------------------------------------------------

    @Test
    @DisplayName("anonymous DELETE → 401 envelope")
    void anonymousDeleteIs401() throws Exception {
        mockMvc.perform(delete(ENDPOINT + "/10"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errors[0].code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("invalid JWT → 401")
    void invalidJwtIs401() throws Exception {
        mockMvc.perform(delete(ENDPOINT + "/10")
                        .header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------
    // Candidate ownership
    // ------------------------------------------------------------------

    @Test
    @DisplayName("candidate deletes own ACTIVE resume → 204; metadata + bytes removed, storage key server-resolved")
    void candidateDeletesOwnActiveResume() throws Exception {
        givenCandidate(2L, "candidate@test.local", 50L, role("CANDIDATE"));
        when(resumeRepository.findById(10L)).thenReturn(Optional.of(resume(10L, 50L, true)));

        mockMvc.perform(delete(ENDPOINT + "/10")
                        .header("Authorization", "Bearer " + tokenFor(2L, "candidate@test.local")))
                .andExpect(status().isNoContent());

        InOrder order = inOrder(resumeRepository, fileStorage);
        order.verify(resumeRepository).delete(any(Resume.class));
        order.verify(resumeRepository).flush();
        order.verify(fileStorage).delete("resumes/10/test-key"); // server-resolved key only
    }

    @Test
    @DisplayName("candidate cannot delete ANOTHER candidate's resume → 404, storage untouched")
    void foreignResumeIsMasked404() throws Exception {
        givenCandidate(2L, "candidate@test.local", 50L, role("CANDIDATE"));
        when(resumeRepository.findById(10L)).thenReturn(Optional.of(resume(10L, 99L, true)));

        mockMvc.perform(delete(ENDPOINT + "/10")
                        .header("Authorization", "Bearer " + tokenFor(2L, "candidate@test.local")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errors[0].code").value("NOT_FOUND"));

        verify(resumeRepository, never()).delete(any(Resume.class));
        verify(fileStorage, never()).delete(anyString());
    }

    @Test
    @DisplayName("candidate cannot delete own INACTIVE resume → 404 (history rows are not a self-service surface)")
    void ownInactiveResumeIs404() throws Exception {
        givenCandidate(2L, "candidate@test.local", 50L, role("CANDIDATE"));
        when(resumeRepository.findById(10L)).thenReturn(Optional.of(resume(10L, 50L, false)));

        mockMvc.perform(delete(ENDPOINT + "/10")
                        .header("Authorization", "Bearer " + tokenFor(2L, "candidate@test.local")))
                .andExpect(status().isNotFound());

        verify(resumeRepository, never()).delete(any(Resume.class));
        verify(fileStorage, never()).delete(anyString());
    }

    @Test
    @DisplayName("candidateId/userId query parameters cannot override ownership")
    void queryParameterOverridesIgnored() throws Exception {
        givenCandidate(2L, "candidate@test.local", 50L, role("CANDIDATE"));
        when(resumeRepository.findById(10L)).thenReturn(Optional.of(resume(10L, 99L, true)));

        mockMvc.perform(delete(ENDPOINT + "/10")
                        .param("candidateId", "50")
                        .param("userId", "2")
                        .header("Authorization", "Bearer " + tokenFor(2L, "candidate@test.local")))
                .andExpect(status().isNotFound());

        verify(resumeRepository, never()).delete(any(Resume.class));
    }

    // ------------------------------------------------------------------
    // Authorization matrix
    // ------------------------------------------------------------------

    @Test
    @DisplayName("admin with candidates:manage deletes ANY resume (incl. inactive) → 204")
    void adminWithPermissionDeletesAny() throws Exception {
        givenCandidate(4L, "admin@test.local", 0L, role("ADMIN", "candidates:manage"));
        when(resumeRepository.findById(10L)).thenReturn(Optional.of(resume(10L, 99L, false)));

        mockMvc.perform(delete(ENDPOINT + "/10")
                        .header("Authorization", "Bearer " + tokenFor(4L, "admin@test.local")))
                .andExpect(status().isNoContent());

        verify(resumeRepository).delete(any(Resume.class));
        verify(fileStorage).delete("resumes/10/test-key");
    }

    @Test
    @DisplayName("admin WITHOUT candidates:manage is treated as a normal user → 404 on foreign resume")
    void adminWithoutPermissionIsNotAdmin() throws Exception {
        givenCandidate(4L, "admin@test.local", 0L, role("ADMIN"));
        when(resumeRepository.findById(10L)).thenReturn(Optional.of(resume(10L, 99L, true)));

        mockMvc.perform(delete(ENDPOINT + "/10")
                        .header("Authorization", "Bearer " + tokenFor(4L, "admin@test.local")))
                .andExpect(status().isNotFound());

        verify(resumeRepository, never()).delete(any(Resume.class));
    }

    @Test
    @DisplayName("employer (no candidate profile) → 404, nothing deleted")
    void employerCannotDelete() throws Exception {
        when(userRepository.findByEmail("employer@test.local")).thenReturn(Optional.of(
                dbUser(3L, "employer@test.local", role("EMPLOYER"))));
        when(candidateRepository.findByUserId(3L)).thenReturn(Optional.empty());
        when(resumeRepository.findById(10L)).thenReturn(Optional.of(resume(10L, 99L, true)));

        mockMvc.perform(delete(ENDPOINT + "/10")
                        .header("Authorization", "Bearer " + tokenFor(3L, "employer@test.local")))
                .andExpect(status().isNotFound());

        verify(resumeRepository, never()).delete(any(Resume.class));
        verify(fileStorage, never()).delete(anyString());
    }

    // ------------------------------------------------------------------
    // Not found / repeated delete / storage behavior
    // ------------------------------------------------------------------

    @Test
    @DisplayName("nonexistent resume → 404 envelope")
    void nonexistentResumeIs404() throws Exception {
        givenCandidate(2L, "candidate@test.local", 50L, role("CANDIDATE"));
        when(resumeRepository.findById(404L)).thenReturn(Optional.empty());

        mockMvc.perform(delete(ENDPOINT + "/404")
                        .header("Authorization", "Bearer " + tokenFor(2L, "candidate@test.local")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errors[0].code").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("repeated DELETE on an already-deleted resume → 404 (no second deletion state)")
    void repeatedDeleteIs404() throws Exception {
        givenCandidate(2L, "candidate@test.local", 50L, role("CANDIDATE"));
        // First call removed the row; the second finds nothing.
        when(resumeRepository.findById(10L)).thenReturn(Optional.empty());

        mockMvc.perform(delete(ENDPOINT + "/10")
                        .header("Authorization", "Bearer " + tokenFor(2L, "candidate@test.local")))
                .andExpect(status().isNotFound());

        verify(resumeRepository, never()).delete(any(Resume.class));
    }

    @Test
    @DisplayName("legacy row without a storage key still deletes metadata safely")
    void missingStorageKeyDeletesMetadata() throws Exception {
        givenCandidate(2L, "candidate@test.local", 50L, role("CANDIDATE"));
        Resume legacy = resume(10L, 50L, true);
        legacy.setStorageKey(null);
        when(resumeRepository.findById(10L)).thenReturn(Optional.of(legacy));

        mockMvc.perform(delete(ENDPOINT + "/10")
                .header("Authorization", "Bearer " + tokenFor(2L, "candidate@test.local")))
                .andExpect(status().isNoContent());

        verify(resumeRepository).delete(any(Resume.class));
        verify(fileStorage, never()).delete(anyString());
    }

    @Test
    @DisplayName("storage delete failure surfaces as a 500 — deletion is never falsely reported")
    void storageFailureIsSafe() throws Exception {
        givenCandidate(2L, "candidate@test.local", 50L, role("CANDIDATE"));
        when(resumeRepository.findById(10L)).thenReturn(Optional.of(resume(10L, 50L, true)));
        org.mockito.Mockito.doThrow(new RuntimeException("storage down"))
                .when(fileStorage).delete(anyString());

        mockMvc.perform(delete(ENDPOINT + "/10")
                        .header("Authorization", "Bearer " + tokenFor(2L, "candidate@test.local")))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errors[0].code").value("INTERNAL_ERROR"));

        verify(resumeRepository, org.mockito.Mockito.atLeastOnce()).delete(any(Resume.class));
    }

    // ------------------------------------------------------------------
    // Response security
    // ------------------------------------------------------------------

    @Test
    @DisplayName("204 response carries no storageKey, no file_url marker, no internals")
    void successResponseExposesNoInternals() throws Exception {
        givenCandidate(2L, "candidate@test.local", 50L, role("CANDIDATE"));
        when(resumeRepository.findById(10L)).thenReturn(Optional.of(resume(10L, 50L, true)));

        var result = mockMvc.perform(delete(ENDPOINT + "/10")
                        .header("Authorization", "Bearer " + tokenFor(2L, "candidate@test.local")))
                .andExpect(status().isNoContent())
                .andReturn();

        String allHeaders = result.getResponse().getHeaderNames().stream()
                .map(name -> name + ": " + result.getResponse().getHeader(name))
                .reduce("", String::concat);

        org.assertj.core.api.Assertions.assertThat(new String(
                        result.getResponse().getContentAsByteArray(), java.nio.charset.StandardCharsets.ISO_8859_1))
                .doesNotContain("resumes/10/test-key")
                .doesNotContain("internal:db-blob");
        org.assertj.core.api.Assertions.assertThat(allHeaders)
                .doesNotContain("resumes/10/test-key")
                .doesNotContain("internal:db-blob");
    }

    @Test
    @DisplayName("error responses never leak storage internals (foreign resume, masked 404)")
    void errorResponseExposesNoInternals() throws Exception {
        givenCandidate(2L, "candidate@test.local", 50L, role("CANDIDATE"));
        when(resumeRepository.findById(10L)).thenReturn(Optional.of(resume(10L, 99L, true)));

        String body = mockMvc.perform(delete(ENDPOINT + "/10")
                        .header("Authorization", "Bearer " + tokenFor(2L, "candidate@test.local")))
                .andExpect(status().isNotFound())
                .andReturn().getResponse().getContentAsString();

        org.assertj.core.api.Assertions.assertThat(body)
                .doesNotContain("resumes/10/test-key")
                .doesNotContain("internal:db-blob")
                .doesNotContain("candidate_id")
                .doesNotContain("storage");
    }

    // ------------------------------------------------------------------
    // Lifecycle invariants
    // ------------------------------------------------------------------

    @Test
    @DisplayName("deleting the active resume leaves zero active rows — a valid domain state; upload path stays intact")
    void activeDeletionPreservesInvariants() throws Exception {
        givenCandidate(2L, "candidate@test.local", 50L, role("CANDIDATE"));
        when(resumeRepository.findById(10L)).thenReturn(Optional.of(resume(10L, 50L, true)));

        mockMvc.perform(delete(ENDPOINT + "/10")
                        .header("Authorization", "Bearer " + tokenFor(2L, "candidate@test.local")))
                .andExpect(status().isNoContent());

        // Delete removes the row itself — it never manipulates the active
        // flag, so the uq_resumes_one_active_per_candidate invariant and the
        // A7.6.2 deactivate-before-insert upload lifecycle are untouched.
        verify(resumeRepository, never()).deactivateActiveResume(anyLong());
        InOrder order = inOrder(resumeRepository, fileStorage);
        order.verify(resumeRepository).delete(any(Resume.class));
        order.verify(resumeRepository).flush();
        order.verify(fileStorage).delete(anyString());
    }
}
