package com.lokmit.foundation.employment.resume.controller;

import com.lokmit.foundation.common.constants.ApiPaths;
import com.lokmit.foundation.employment.candidate.entity.Candidate;
import com.lokmit.foundation.employment.candidate.repository.CandidateRepository;
import com.lokmit.foundation.employment.resume.entity.Resume;
import com.lokmit.foundation.employment.resume.repository.ResumeRepository;
import com.lokmit.foundation.employment.resume.service.ResumeDownloadService;
import com.lokmit.foundation.employment.resume.service.ResumeDeleteService;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A7.6.4 API security matrix for GET /api/v1/resumes/{resumeId}/download,
 * through the REAL Spring Security filter chain (real JWT filter, real
 * token provider, repository-stubbed users — no database). The REAL
 * {@link ResumeDownloadService} runs with storage mocked at the
 * {@link FileStorage} boundary, so authorization, active-rule and
 * integrity decisions are exercised end-to-end.
 */
@WebMvcTest(controllers = ResumeDownloadController.class)
@Import({SecurityConfig.class, CorsConfig.class, JwtKeyConfig.class,
        JwtTokenProvider.class, CustomUserDetailsService.class, SecurityUtils.class,
        ResumeDownloadService.class, ResumeDeleteService.class, ResumeOwnershipService.class})
@AutoConfigureMockMvc
class ResumeDownloadApiSecurityTest {

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

    private static final byte[] PDF_BYTES = "%PDF-1.4\n%body\n%%EOF\n".getBytes(StandardCharsets.US_ASCII);
    private static final String PDF_SHA256 = sha256Hex(PDF_BYTES);

    /**
     * Independent test-side digest oracle for EXPECTED checksum values —
     * production code keeps its single implementation in A7.6.2; the
     * service under test verifies stored bytes against metadata.
     */
    private static String sha256Hex(byte[] content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return java.util.HexFormat.of().formatHex(digest.digest(content));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    // ------------------------------------------------------------------
    // Fixtures
    // ------------------------------------------------------------------

    private User dbUser(long id, String email, Role... roles) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setPasswordHash("hash-not-used-by-jwt-filter-0123456789");
        user.setFullName("Test User");
        user.setUserType(roles.length > 0 && "ADMIN".equals(roles[0].getCode()) ? "STAFF" : "CANDIDATE");
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

    private void givenUserWithProfile(long userId, String email, long candidateId, Role... roles) {
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(dbUser(userId, email, roles)));
        Candidate c = new Candidate();
        c.setId(candidateId);
        c.setAvailabilityStatus(Candidate.AVAILABILITY_ACTIVELY_LOOKING);
        c.setCreatedAt(OffsetDateTime.now());
        c.setUpdatedAt(OffsetDateTime.now());
        when(candidateRepository.findByUserId(userId)).thenReturn(Optional.of(c));
    }

    private void givenCandidateResume(long resumeId, long candidateId, boolean active) {
        Resume resume = new Resume();
        resume.setId(resumeId);
        resume.setCandidateId(candidateId);
        resume.setFileName("cv.pdf");
        resume.setFileType("application/pdf");
        resume.setFileSizeBytes((long) PDF_BYTES.length);
        resume.setActive(active);
        resume.setCreatedAt(OffsetDateTime.now());
        resume.setChecksumSha256(PDF_SHA256);
        resume.setStorageKey("resumes/" + resumeId + "/test-key");
        when(resumeRepository.findById(resumeId)).thenReturn(Optional.of(resume));
        when(fileStorage.load("resumes/" + resumeId + "/test-key")).thenReturn(PDF_BYTES);
    }

    private String tokenFor(long userId, String email) {
        return jwtTokenProvider.generateAccessToken(userId, email, List.of());
    }

    // ------------------------------------------------------------------
    // Authentication
    // ------------------------------------------------------------------

    @Test
    @DisplayName("anonymous download → 401 envelope")
    void anonymousDownloadIs401() throws Exception {
        mockMvc.perform(get(ENDPOINT + "/10/download"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errors[0].code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("invalid JWT → 401")
    void invalidJwtIs401() throws Exception {
        mockMvc.perform(get(ENDPOINT + "/10/download")
                        .header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------
    // Candidate ownership
    // ------------------------------------------------------------------

    @Test
    @DisplayName("candidate downloads own ACTIVE resume → 200, exact bytes, safe headers")
    void candidateDownloadsOwnActiveResume() throws Exception {
        givenUserWithProfile(2L, "candidate@test.local", 50L, role("CANDIDATE"));
        givenCandidateResume(10L, 50L, true);

        mockMvc.perform(get(ENDPOINT + "/10/download")
                        .header("Authorization", "Bearer " + tokenFor(2L, "candidate@test.local")))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(header().string("Pragma", "no-cache"))
                .andExpect(content().contentTypeCompatibleWith("application/pdf"))
                .andExpect(content().bytes(PDF_BYTES))
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"cv.pdf\"; filename*=UTF-8''cv.pdf"));

        verify(fileStorage).load("resumes/10/test-key");
    }

    @Test
    @DisplayName("candidate cannot download ANOTHER candidate's resume → 404 (masked)")
    void foreignResumeIs404() throws Exception {
        givenUserWithProfile(2L, "candidate@test.local", 50L, role("CANDIDATE"));
        givenCandidateResume(10L, 99L, true); // owned by candidate 99

        mockMvc.perform(get(ENDPOINT + "/10/download")
                        .header("Authorization", "Bearer " + tokenFor(2L, "candidate@test.local")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errors[0].code").value("NOT_FOUND"));

        // Existence masking: storage is never touched for a foreign resume.
        verify(fileStorage, never()).load(anyString());
    }

    @Test
    @DisplayName("candidate cannot retrieve another candidate's INACTIVE resume → 404")
    void inactiveForeignResumeIs404() throws Exception {
        givenUserWithProfile(2L, "candidate@test.local", 50L, role("CANDIDATE"));
        givenCandidateResume(10L, 99L, false);

        mockMvc.perform(get(ENDPOINT + "/10/download")
                        .header("Authorization", "Bearer " + tokenFor(2L, "candidate@test.local")))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("candidate cannot access their OWN inactive resume → 404")
    void ownInactiveResumeIs404() throws Exception {
        givenUserWithProfile(2L, "candidate@test.local", 50L, role("CANDIDATE"));
        givenCandidateResume(10L, 50L, false);

        mockMvc.perform(get(ENDPOINT + "/10/download")
                        .header("Authorization", "Bearer " + tokenFor(2L, "candidate@test.local")))
                .andExpect(status().isNotFound());

        verify(fileStorage, never()).load(anyString());
    }

    @Test
    @DisplayName("ownership cannot be overridden with query parameters → owner stays server-derived")
    void ownershipCannotBeOverridden() throws Exception {
        givenUserWithProfile(2L, "candidate@test.local", 50L, role("CANDIDATE"));
        givenCandidateResume(10L, 99L, true);

        mockMvc.perform(get(ENDPOINT + "/10/download")
                        .param("candidateId", "99")
                        .param("userId", "99")
                        .header("Authorization", "Bearer " + tokenFor(2L, "candidate@test.local")))
                .andExpect(status().isNotFound());

        verify(fileStorage, never()).load(anyString());
    }

    // ------------------------------------------------------------------
    // Authorization matrix
    // ------------------------------------------------------------------

    @Test
    @DisplayName("admin with candidates:manage downloads any resume (incl. inactive) → 200")
    void adminWithPermissionDownloadsAny() throws Exception {
        givenUserWithProfile(4L, "admin@test.local", 0L, role("ADMIN", "candidates:manage"));
        givenCandidateResume(10L, 99L, false);

        mockMvc.perform(get(ENDPOINT + "/10/download")
                        .header("Authorization", "Bearer " + tokenFor(4L, "admin@test.local")))
                .andExpect(status().isOk())
                .andExpect(content().bytes(PDF_BYTES));

        verify(fileStorage).load("resumes/10/test-key");
    }

    @Test
    @DisplayName("admin WITHOUT candidates:manage is treated as a non-admin (404 on foreign resume)")
    void adminWithoutManagePermissionIsNotAdmin() throws Exception {
        givenUserWithProfile(4L, "admin@test.local", 0L, role("ADMIN"));
        givenCandidateResume(10L, 99L, true);

        mockMvc.perform(get(ENDPOINT + "/10/download")
                        .header("Authorization", "Bearer " + tokenFor(4L, "admin@test.local")))
                .andExpect(status().isNotFound());

        verify(fileStorage, never()).load(anyString());
    }

    @Test
    @DisplayName("employer (authenticated, no candidate profile, no permission) → 404, no storage access")
    void employerHasNoAccess() throws Exception {
        when(userRepository.findByEmail("employer@test.local")).thenReturn(Optional.of(
                dbUser(3L, "employer@test.local", role("EMPLOYER"))));
        when(candidateRepository.findByUserId(3L)).thenReturn(Optional.empty());
        givenCandidateResume(10L, 99L, true);

        mockMvc.perform(get(ENDPOINT + "/10/download")
                        .header("Authorization", "Bearer " + tokenFor(3L, "employer@test.local")))
                .andExpect(status().isNotFound());

        verify(fileStorage, never()).load(anyString());
    }

    @Test
    @DisplayName("admin with candidates:manage but no candidate profile still gets admin path → 200")
    void adminWithoutCandidateProfileKeepsAdminPath() throws Exception {
        when(userRepository.findByEmail("admin2@test.local")).thenReturn(Optional.of(
                dbUser(5L, "admin2@test.local", role("ADMIN", "candidates:manage"))));
        when(candidateRepository.findByUserId(5L)).thenReturn(Optional.empty());
        givenCandidateResume(10L, 99L, true);

        mockMvc.perform(get(ENDPOINT + "/10/download")
                        .header("Authorization", "Bearer " + tokenFor(5L, "admin2@test.local")))
                .andExpect(status().isOk());
    }

    // ------------------------------------------------------------------
    // Not found / storage failure / integrity
    // ------------------------------------------------------------------

    @Test
    @DisplayName("nonexistent resume → 404 envelope")
    void nonexistentResumeIs404() throws Exception {
        givenUserWithProfile(2L, "candidate@test.local", 50L, role("CANDIDATE"));
        when(resumeRepository.findById(404L)).thenReturn(Optional.empty());

        mockMvc.perform(get(ENDPOINT + "/404/download")
                        .header("Authorization", "Bearer " + tokenFor(2L, "candidate@test.local")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errors[0].code").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("storage failure → safe 500 INTERNAL_ERROR envelope, no internals leak")
    void storageFailureIsSafe500() throws Exception {
        givenUserWithProfile(2L, "candidate@test.local", 50L, role("CANDIDATE"));
        givenCandidateResume(10L, 50L, true);
        when(fileStorage.load("resumes/10/test-key"))
                .thenThrow(new RuntimeException("jdbc:postgresql://secret-host/db blew up"));

        mockMvc.perform(get(ENDPOINT + "/10/download")
                        .header("Authorization", "Bearer " + tokenFor(2L, "candidate@test.local")))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errors[0].code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("Resume file could not be retrieved"));
    }

    @Test
    @DisplayName("checksum mismatch → safe 500, bytes never served")
    void checksumMismatchFailsClosed() throws Exception {
        givenUserWithProfile(2L, "candidate@test.local", 50L, role("CANDIDATE"));
        givenCandidateResume(10L, 50L, true);
        when(fileStorage.load("resumes/10/test-key"))
                .thenReturn("<tampered/>".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(get(ENDPOINT + "/10/download")
                        .header("Authorization", "Bearer " + tokenFor(2L, "candidate@test.local")))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errors[0].code").value("INTERNAL_ERROR"));
    }

    @Test
    @DisplayName("resume without recorded checksum → fail closed (500), bytes never served")
    void missingChecksumFailsClosed() throws Exception {
        givenUserWithProfile(2L, "candidate@test.local", 50L, role("CANDIDATE"));
        givenCandidateResume(10L, 50L, true);
        when(resumeRepository.findById(10L)).thenAnswer(inv -> {
            Resume r = new Resume();
            r.setId(10L);
            r.setCandidateId(50L);
            r.setFileName("cv.pdf");
            r.setFileType("application/pdf");
            r.setActive(true);
            r.setCreatedAt(OffsetDateTime.now());
            r.setChecksumSha256(null);
            r.setStorageKey("resumes/10/test-key");
            return Optional.of(r);
        });

        mockMvc.perform(get(ENDPOINT + "/10/download")
                        .header("Authorization", "Bearer " + tokenFor(2L, "candidate@test.local")))
                .andExpect(status().isInternalServerError());
    }

    // ------------------------------------------------------------------
    // Response shape for the three formats
    // ------------------------------------------------------------------

    @Test
    @DisplayName("DOC bytes → application/msword attachment")
    void docContentType() throws Exception {
        givenUserWithProfile(2L, "candidate@test.local", 50L, role("CANDIDATE"));
        byte[] doc = new byte[]{(byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0,
                (byte) 0xA1, (byte) 0xB1, 0x1A, (byte) 0xE1, 'd', 'o', 'c'};
        Resume resume = new Resume();
        resume.setId(11L);
        resume.setCandidateId(50L);
        resume.setFileName("cv.doc");
        resume.setFileType("application/msword");
        resume.setActive(true);
        resume.setCreatedAt(OffsetDateTime.now());
        resume.setChecksumSha256(sha256Hex(doc));
        resume.setStorageKey("resumes/11/test-key");
        when(resumeRepository.findById(11L)).thenReturn(Optional.of(resume));
        when(fileStorage.load("resumes/11/test-key")).thenReturn(doc);

        mockMvc.perform(get(ENDPOINT + "/11/download")
                        .header("Authorization", "Bearer " + tokenFor(2L, "candidate@test.local")))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/msword"))
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"cv.doc\"; filename*=UTF-8''cv.doc"));
    }

    @Test
    @DisplayName("DOCX bytes → wordprocessingml attachment; hostile filename is header-injection-safe")
    void docxContentTypeAndSafeDisposition() throws Exception {
        givenUserWithProfile(2L, "candidate@test.local", 50L, role("CANDIDATE"));
        byte[] docx = "<docx/>".getBytes(StandardCharsets.UTF_8);
        Resume resume = new Resume();
        resume.setId(12L);
        resume.setCandidateId(50L);
        // Filename is A7.6.2-validated at upload; here we prove the
        // disposition encoder cannot be abused even by a hostile stored name.
        resume.setFileName("r\u00e9sum\u00e9 v2.docx");
        resume.setFileType(
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        resume.setActive(true);
        resume.setCreatedAt(OffsetDateTime.now());
        resume.setChecksumSha256(sha256Hex(docx));
        resume.setStorageKey("resumes/12/test-key");
        when(resumeRepository.findById(12L)).thenReturn(Optional.of(resume));
        when(fileStorage.load("resumes/12/test-key")).thenReturn(docx);

        mockMvc.perform(get(ENDPOINT + "/12/download")
                        .header("Authorization", "Bearer " + tokenFor(2L, "candidate@test.local")))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"r_sum_ v2.docx\"; "
                                + "filename*=UTF-8''r%C3%A9sum%C3%A9%20v2.docx"));
    }

    @Test
    @DisplayName("storageKey and internal marker are never present in any response")
    void noStorageInternalsInResponse() throws Exception {
        givenUserWithProfile(2L, "candidate@test.local", 50L, role("CANDIDATE"));
        givenCandidateResume(10L, 50L, true);

        var result = mockMvc.perform(get(ENDPOINT + "/10/download")
                        .header("Authorization", "Bearer " + tokenFor(2L, "candidate@test.local")))
                .andExpect(status().isOk())
                .andReturn();

        byte[] body = result.getResponse().getContentAsByteArray();
        String allHeaders = result.getResponse().getHeaderNames().stream()
                .map(name -> name + ": " + result.getResponse().getHeader(name))
                .reduce("", String::concat);

        org.assertj.core.api.Assertions.assertThat(new String(body, StandardCharsets.ISO_8859_1))
                .doesNotContain("resumes/10/test-key")
                .doesNotContain("internal:db-blob");
        org.assertj.core.api.Assertions.assertThat(allHeaders)
                .doesNotContain("resumes/10/test-key")
                .doesNotContain("internal:db-blob");
    }
}
