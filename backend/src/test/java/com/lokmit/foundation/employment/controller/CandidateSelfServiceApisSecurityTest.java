package com.lokmit.foundation.employment.controller;

import com.lokmit.foundation.common.constants.ApiPaths;
import com.lokmit.foundation.employment.candidate.controller.CandidateProfileController;
import com.lokmit.foundation.employment.candidate.entity.Candidate;
import com.lokmit.foundation.employment.candidate.repository.CandidateRepository;
import com.lokmit.foundation.employment.candidate.service.CandidateService;
import com.lokmit.foundation.employment.candidate.dto.CandidateResponse;
import com.lokmit.foundation.employment.resume.controller.CandidateResumeController;
import com.lokmit.foundation.employment.resume.dto.ResumeResponse;
import com.lokmit.foundation.employment.resume.entity.Resume;
import com.lokmit.foundation.employment.resume.repository.ResumeRepository;
import com.lokmit.foundation.employment.resume.service.ResumeOwnershipService;
import com.lokmit.foundation.security.config.CorsConfig;
import com.lokmit.foundation.security.config.JwtKeyConfig;
import com.lokmit.foundation.security.config.SecurityConfig;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A10 security matrix for the candidate self-service profile and resume
 * listing endpoints, through the REAL Spring Security filter chain.
 *
 * <p>Coverage: anonymous 401; authenticated candidate reads/updates own
 * data; ownership is server-resolved (a candidateId in the path or body
 * does not exist and cannot redirect ownership); non-candidate users
 * without a candidate profile receive the plain masked 404; responses
 * never expose security material.</p>
 */
@WebMvcTest(controllers = {CandidateProfileController.class, CandidateResumeController.class})
@Import({SecurityConfig.class, CorsConfig.class, JwtKeyConfig.class,
        JwtTokenProvider.class, CustomUserDetailsService.class, SecurityUtils.class,
        CandidateService.class, ResumeOwnershipService.class})
@AutoConfigureMockMvc
class CandidateSelfServiceApisSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CandidateRepository candidateRepository;

    @MockitoBean
    private ResumeRepository resumeRepository;

    @MockitoBean
    private UserRepository userRepository;

    private static final String ME_PROFILE = ApiPaths.CANDIDATE_ME_PROFILE;
    private static final String ME_RESUMES = ApiPaths.CANDIDATE_ME_RESUMES;
    private static final String CANDIDATE_EMAIL = "candidate@lokmitfoundation.org";

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void resetStubs() {
        reset(candidateRepository, resumeRepository, userRepository);
    }

    // ------------------------------------------------------------------
    // fixtures
    // ------------------------------------------------------------------

    private User dbUser(long id, String email) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setPasswordHash("hash-not-used-by-jwt-filter-0123456789");
        user.setFullName("Test Candidate");
        user.setUserType("CANDIDATE");
        user.setStatus("ACTIVE");
        user.setEmailVerified(true);
        user.setCreatedAt(OffsetDateTime.now());
        user.setUpdatedAt(OffsetDateTime.now());
        user.setRoles(Set.of(role("CANDIDATE")));
        return user;
    }

    private Role role(String code) {
        Role role = new Role();
        role.setCode(code);
        return role;
    }

    private Candidate candidate(long id, User user) {
        Candidate c = new Candidate();
        c.setId(id);
        c.setUser(user);
        c.setPhone("+91-9000000001");
        c.setCurrentLocation("Patna");
        c.setAvailabilityStatus("ACTIVELY_LOOKING");
        c.setCreatedAt(OffsetDateTime.now());
        c.setUpdatedAt(OffsetDateTime.now());
        return c;
    }

    private Resume resume(long id, long candidateId, boolean active) {
        Resume r = new Resume();
        r.setId(id);
        r.setCandidateId(candidateId);
        r.setFileUrl("legacy");
        r.setFileName("resume-" + id + ".pdf");
        r.setFileType("application/pdf");
        r.setFileSizeBytes(1200L);
        r.setActive(active);
        r.setCreatedAt(OffsetDateTime.now());
        return r;
    }

    private String tokenFor(long userId, String email) {
        return jwtTokenProvider.generateAccessToken(userId, email, List.of());
    }

    // ------------------------------------------------------------------
    // authentication
    // ------------------------------------------------------------------

    @Test
    @DisplayName("anonymous profile and resume reads are rejected 401")
    void anonymousRequestsRejected() throws Exception {
        mockMvc.perform(get(ME_PROFILE)).andExpect(status().isUnauthorized());
        mockMvc.perform(patch(ME_PROFILE).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"+91-1\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get(ME_RESUMES)).andExpect(status().isUnauthorized());

        verify(candidateRepository, never()).save(any(Candidate.class));
    }

    @Test
    @DisplayName("candidate reads own profile; user without candidate profile gets masked 404")
    void profileOwnershipResolution() throws Exception {
        User user = dbUser(2L, CANDIDATE_EMAIL);
        when(userRepository.findByEmail(CANDIDATE_EMAIL)).thenReturn(Optional.of(user));
        when(candidateRepository.findByUserId(2L))
                .thenReturn(Optional.of(candidate(10L, user)));
        when(candidateRepository.findById(10L)).thenReturn(Optional.of(candidate(10L, user)));

        mockMvc.perform(get(ME_PROFILE)
                        .header("Authorization", "Bearer " + tokenFor(2L, CANDIDATE_EMAIL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(10))
                // No password/lockout/token material ever leaves the backend.
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.data.user").doesNotExist());

        // A user with no candidate profile (admin/employer/client) → 404.
        when(candidateRepository.findByUserId(2L)).thenReturn(Optional.empty());
        mockMvc.perform(get(ME_PROFILE)
                        .header("Authorization", "Bearer " + tokenFor(2L, CANDIDATE_EMAIL)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errors[0].code").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("PATCH updates only the caller's own profile fields")
    void profileUpdateIsOwnershipScoped() throws Exception {
        User user = dbUser(2L, CANDIDATE_EMAIL);
        when(userRepository.findByEmail(CANDIDATE_EMAIL)).thenReturn(Optional.of(user));
        when(candidateRepository.findByUserId(2L))
                .thenReturn(Optional.of(candidate(10L, user)));
        Candidate owned = candidate(10L, user);
        when(candidateRepository.findById(10L)).thenReturn(Optional.of(owned));
        when(candidateRepository.save(any(Candidate.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(patch(ME_PROFILE)
                        .header("Authorization", "Bearer " + tokenFor(2L, CANDIDATE_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"+91-9876543210\",\"gender\":\"OTHER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.phone").value("+91-9876543210"));

        verify(candidateRepository).save(any(Candidate.class));
        // There is no candidateId request field: ownership came from the JWT
        // (user 2 → candidate 10), not from client input.
    }

    @Test
    @DisplayName("resume list returns only the caller's own resumes, metadata only")
    void resumeListIsOwnershipScoped() throws Exception {
        User user = dbUser(2L, CANDIDATE_EMAIL);
        when(userRepository.findByEmail(CANDIDATE_EMAIL)).thenReturn(Optional.of(user));
        when(candidateRepository.findByUserId(2L))
                .thenReturn(Optional.of(candidate(10L, user)));
        when(resumeRepository.findByCandidateIdOrderByCreatedAtDesc(10L))
                .thenReturn(List.of(resume(300L, 10L, true), resume(299L, 10L, false)));

        String body = mockMvc.perform(get(ME_RESUMES)
                        .header("Authorization", "Bearer " + tokenFor(2L, CANDIDATE_EMAIL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].active").value(true))
                .andExpect(jsonPath("$.data[1].active").value(false))
                .andReturn().getResponse().getContentAsString();

        // Storage internals never leak: no storageKey, no fileUrl, no blob info.
        org.assertj.core.api.Assertions.assertThat(body)
                .doesNotContain("storageKey")
                .doesNotContain("fileUrl")
                .doesNotContain("content");
    }

    @Test
    @DisplayName("resume list for a user without a candidate profile is a masked 404")
    void resumeListWithoutProfileMasked404() throws Exception {
        User user = dbUser(2L, CANDIDATE_EMAIL);
        when(userRepository.findByEmail(CANDIDATE_EMAIL)).thenReturn(Optional.of(user));
        when(candidateRepository.findByUserId(2L)).thenReturn(Optional.empty());

        mockMvc.perform(get(ME_RESUMES)
                        .header("Authorization", "Bearer " + tokenFor(2L, CANDIDATE_EMAIL)))
                .andExpect(status().isNotFound());
    }
}
