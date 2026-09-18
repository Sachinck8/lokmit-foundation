package com.lokmit.foundation.employment.resume.controller;

import com.lokmit.foundation.common.constants.ApiPaths;
import com.lokmit.foundation.employment.candidate.entity.Candidate;
import com.lokmit.foundation.employment.candidate.repository.CandidateRepository;
import com.lokmit.foundation.employment.resume.config.UploadProperties;
import com.lokmit.foundation.employment.resume.entity.Resume;
import com.lokmit.foundation.employment.resume.repository.ResumeRepository;
import com.lokmit.foundation.employment.resume.service.ResumeOwnershipService;
import com.lokmit.foundation.employment.resume.service.ResumeUploadService;
import com.lokmit.foundation.employment.resume.service.storage.FileStorage;
import com.lokmit.foundation.employment.resume.service.validation.ResumeContentValidator;
import com.lokmit.foundation.employment.resume.service.validation.ResumeFilenameValidator;
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
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A7.6.3 API security matrix for POST /api/v1/candidates/me/resumes,
 * exercised through the REAL Spring Security filter chain (real JWT filter,
 * real token provider, repository-stubbed user loading — no database).
 * Persistence is mocked at the repository boundary while the REAL A7.6.2
 * validators and upload service run, so what is under test is the API's
 * pass-through of the security decisions plus its ownership resolution.
 */
@WebMvcTest(controllers = ResumeUploadController.class)
@Import({SecurityConfig.class, CorsConfig.class, JwtKeyConfig.class,
        JwtTokenProvider.class, CustomUserDetailsService.class, SecurityUtils.class,
        ResumeUploadService.class, ResumeOwnershipService.class,
        ResumeContentValidator.class, ResumeFilenameValidator.class, UploadProperties.class})
@AutoConfigureMockMvc
class ResumeUploadApiSecurityTest {

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

    private static final String ENDPOINT = ApiPaths.CANDIDATE_ME_RESUMES;

    // ------------------------------------------------------------------
    // Fixtures
    // ------------------------------------------------------------------

    private User dbUser(long id, String email, String userType, Role... roles) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setPasswordHash("hash-not-used-by-jwt-filter-0123456789");
        user.setFullName("Test User");
        user.setUserType(userType);
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

    private Candidate candidate(long id, long userId) {
        Candidate c = new Candidate();
        c.setId(id);
        c.setUser(dbUser(userId, "owner-" + id + "@test.local", "CANDIDATE"));
        c.setAvailabilityStatus(Candidate.AVAILABILITY_ACTIVELY_LOOKING);
        c.setCreatedAt(OffsetDateTime.now());
        c.setUpdatedAt(OffsetDateTime.now());
        return c;
    }

    private void givenAuthenticatedCandidate(long userId, String email) {
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(
                dbUser(userId, email, "CANDIDATE", role("CANDIDATE"))));
        when(candidateRepository.findByUserId(userId)).thenReturn(Optional.of(
                candidate(50L, userId)));
    }

    private void givenPersistenceAcceptsUploads() {
        // A7.6.2 re-verifies candidate existence inside the upload transaction.
        when(candidateRepository.existsById(50L)).thenReturn(true);
        when(resumeRepository.saveAndFlush(any(Resume.class))).thenAnswer(inv -> {
            Resume r = inv.getArgument(0);
            if (r.getId() == null) {
                r.setId(77L);
            }
            return r;
        });
        when(resumeRepository.deactivateActiveResume(50L)).thenReturn(0);
    }

    private String tokenFor(long userId, String email) {
        return jwtTokenProvider.generateAccessToken(userId, email, List.of("CANDIDATE"));
    }

    private static MockMultipartFile file(byte[] content, String filename, String mime) {
        return new MockMultipartFile("file", filename, mime, content);
    }

    private static byte[] validPdf() {
        return "%PDF-1.4\n%body\n%%EOF\n".getBytes(StandardCharsets.US_ASCII);
    }

    private static final String WORD_CT =
            "<?xml version=\"1.0\"?><Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
                    + "<Default Extension=\"xml\" ContentType=\"application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml\"/>"
                    + "</Types>";

    private static byte[] validDocx() {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            ZipOutputStream zip = new ZipOutputStream(bytes);
            zip.putNextEntry(new ZipEntry("[Content_Types].xml"));
            zip.write(WORD_CT.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry("word/document.xml"));
            zip.write("<w:document/>".getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
            zip.close();
            return bytes.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    // ------------------------------------------------------------------
    // Authentication
    // ------------------------------------------------------------------

    @Test
    @DisplayName("unauthenticated upload → 401 envelope")
    void anonymousUploadIs401() throws Exception {
        mockMvc.perform(multipart(ENDPOINT)
                        .file(file(validPdf(), "cv.pdf", "application/pdf")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errors[0].code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("invalid JWT → 401")
    void invalidJwtIs401() throws Exception {
        mockMvc.perform(multipart(ENDPOINT)
                        .file(file(validPdf(), "cv.pdf", "application/pdf"))
                        .header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------
    // Ownership / IDOR
    // ------------------------------------------------------------------

    @Test
    @DisplayName("authenticated candidate uploads → 201, safe DTO only")
    void candidateUploadSucceeds201() throws Exception {
        givenAuthenticatedCandidate(2L, "candidate@test.local");
        givenPersistenceAcceptsUploads();

        mockMvc.perform(multipart(ENDPOINT)
                        .file(file(validPdf(), "my-resume.pdf", "application/pdf"))
                        .header("Authorization", "Bearer " + tokenFor(2L, "candidate@test.local")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(77))
                .andExpect(jsonPath("$.data.candidateId").value(50))
                .andExpect(jsonPath("$.data.fileName").value("my-resume.pdf"))
                .andExpect(jsonPath("$.data.fileType").value("application/pdf"))
                .andExpect(jsonPath("$.data.active").value(true))
                .andExpect(jsonPath("$.data.checksumSha256").exists())
                // Response security: storage internals never leak.
                .andExpect(jsonPath("$.data.storageKey").doesNotExist())
                .andExpect(jsonPath("$.data.fileUrl").doesNotExist())
                .andExpect(jsonPath("$.data.content").doesNotExist());
    }

    @Test
    @DisplayName("authenticated user with NO candidate profile → 404 (no existence leak)")
    void userWithoutCandidateProfileIs404() throws Exception {
        when(userRepository.findByEmail("employer@test.local")).thenReturn(Optional.of(
                dbUser(3L, "employer@test.local", "EMPLOYER", role("EMPLOYER"))));
        when(candidateRepository.findByUserId(3L)).thenReturn(Optional.empty());

        mockMvc.perform(multipart(ENDPOINT)
                        .file(file(validPdf(), "cv.pdf", "application/pdf"))
                        .header("Authorization", "Bearer " + tokenFor(3L, "employer@test.local")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errors[0].code").value("NOT_FOUND"));

        verify(resumeRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("no IDOR: smuggled candidateId params are ignored; owner is server-derived")
    void noIdorPath() throws Exception {
        givenAuthenticatedCandidate(2L, "candidate@test.local");
        givenPersistenceAcceptsUploads();

        mockMvc.perform(multipart(ENDPOINT)
                        .file(file(validPdf(), "cv.pdf", "application/pdf"))
                        .param("candidateId", "999")
                        .param("candidate_id", "999")
                        .header("Authorization", "Bearer " + tokenFor(2L, "candidate@test.local")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.candidateId").value(50));

        // Deactivation and persistence ran against the OWN candidate (50), never 999.
        verify(resumeRepository).deactivateActiveResume(50L);
        ArgumentCaptor<Resume> captor = ArgumentCaptor.forClass(Resume.class);
        verify(resumeRepository, org.mockito.Mockito.times(2)).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getCandidateId()).isEqualTo(50L);
    }

    // ------------------------------------------------------------------
    // Upload validation pass-through (A7.6.2 is the authority)
    // ------------------------------------------------------------------

    @Test
    @DisplayName("valid DOCX upload → 201 with detected content type")
    void docxUploadSucceeds() throws Exception {
        givenAuthenticatedCandidate(2L, "candidate@test.local");
        givenPersistenceAcceptsUploads();

        mockMvc.perform(multipart(ENDPOINT)
                        .file(file(validDocx(), "cv.docx",
                                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                        .header("Authorization", "Bearer " + tokenFor(2L, "candidate@test.local")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.fileType")
                        .value("application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
    }

    @Test
    @DisplayName("renamed executable content as .pdf → 400, nothing persisted")
    void renamedContentRejected() throws Exception {
        givenAuthenticatedCandidate(2L, "candidate@test.local");

        byte[] evil = "MZ\220\0not-a-resume".getBytes(StandardCharsets.ISO_8859_1);
        mockMvc.perform(multipart(ENDPOINT)
                        .file(file(evil, "evil.pdf", "application/pdf"))
                        .header("Authorization", "Bearer " + tokenFor(2L, "candidate@test.local")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].code").value("BAD_REQUEST"));

        verify(resumeRepository, never()).saveAndFlush(any());
        verify(fileStorage, never()).store(anyString(), any(byte[].class));
    }

    @Test
    @DisplayName("invalid filename (path traversal) → 400, nothing persisted")
    void invalidFilenameRejected() throws Exception {
        givenAuthenticatedCandidate(2L, "candidate@test.local");

        mockMvc.perform(multipart(ENDPOINT)
                        .file(file(validPdf(), "../evil.pdf", "application/pdf"))
                        .header("Authorization", "Bearer " + tokenFor(2L, "candidate@test.local")))
                .andExpect(status().isBadRequest());

        verify(resumeRepository, never()).saveAndFlush(any());
        verify(fileStorage, never()).store(anyString(), any(byte[].class));
    }

    @Test
    @DisplayName("empty file → 400, nothing persisted")
    void emptyFileRejected() throws Exception {
        givenAuthenticatedCandidate(2L, "candidate@test.local");

        mockMvc.perform(multipart(ENDPOINT)
                        .file(file(new byte[0], "empty.pdf", "application/pdf"))
                        .header("Authorization", "Bearer " + tokenFor(2L, "candidate@test.local")))
                .andExpect(status().isBadRequest());

        verify(resumeRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("oversized (>5 MiB) → 413 PAYLOAD_TOO_LARGE, nothing persisted")
    void oversizedRejected413() throws Exception {
        givenAuthenticatedCandidate(2L, "candidate@test.local");

        byte[] big = new byte[5 * 1024 * 1024 + 1];
        big[0] = '%';
        big[1] = 'P';

        mockMvc.perform(multipart(ENDPOINT)
                        .file(file(big, "big.pdf", "application/pdf"))
                        .header("Authorization", "Bearer " + tokenFor(2L, "candidate@test.local")))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.errors[0].code").value("PAYLOAD_TOO_LARGE"));

        verify(resumeRepository, never()).saveAndFlush(any());
        verify(fileStorage, never()).store(anyString(), any(byte[].class));
    }

    @Test
    @DisplayName("contradictory MIME → 400 (content detection wins)")
    void contradictoryMimeRejected() throws Exception {
        givenAuthenticatedCandidate(2L, "candidate@test.local");

        mockMvc.perform(multipart(ENDPOINT)
                        .file(file(validPdf(), "cv.pdf", "application/zip"))
                        .header("Authorization", "Bearer " + tokenFor(2L, "candidate@test.local")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].code").value("BAD_REQUEST"));
    }

    @Test
    @DisplayName("neutral application/octet-stream accepted (A7.6.2 semantics)")
    void octetStreamAccepted() throws Exception {
        givenAuthenticatedCandidate(2L, "candidate@test.local");
        givenPersistenceAcceptsUploads();

        mockMvc.perform(multipart(ENDPOINT)
                        .file(file(validPdf(), "cv.pdf", "application/octet-stream"))
                        .header("Authorization", "Bearer " + tokenFor(2L, "candidate@test.local")))
                .andExpect(status().isCreated());
    }

    // ------------------------------------------------------------------
    // Replacement semantics
    // ------------------------------------------------------------------

    @Test
    @DisplayName("second upload deactivates the prior active resume through the service")
    void secondUploadReplacesActive() throws Exception {
        givenAuthenticatedCandidate(2L, "candidate@test.local");
        givenPersistenceAcceptsUploads();
        when(resumeRepository.deactivateActiveResume(50L)).thenReturn(1);

        mockMvc.perform(multipart(ENDPOINT)
                        .file(file(validPdf(), "new.pdf", "application/pdf"))
                        .header("Authorization", "Bearer " + tokenFor(2L, "candidate@test.local")))
                .andExpect(status().isCreated());

        verify(resumeRepository).deactivateActiveResume(50L);
    }

    // ------------------------------------------------------------------
    // Authorization matrix (non-candidate roles)
    // ------------------------------------------------------------------

    @Test
    @DisplayName("admin with no candidate profile → 404, same as any profile-less user")
    void adminWithoutProfileIs404() throws Exception {
        when(userRepository.findByEmail("admin@test.local")).thenReturn(Optional.of(
                dbUser(4L, "admin@test.local", "STAFF",
                        role("ADMIN", "candidates:manage"))));
        when(candidateRepository.findByUserId(4L)).thenReturn(Optional.empty());

        mockMvc.perform(multipart(ENDPOINT)
                        .file(file(validPdf(), "cv.pdf", "application/pdf"))
                        .header("Authorization", "Bearer " + tokenFor(4L, "admin@test.local")))
                .andExpect(status().isNotFound());

        verify(resumeRepository, never()).saveAndFlush(any());
    }
}
