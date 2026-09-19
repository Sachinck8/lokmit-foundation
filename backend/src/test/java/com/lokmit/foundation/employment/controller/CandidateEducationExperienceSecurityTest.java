package com.lokmit.foundation.employment.controller;

import com.lokmit.foundation.common.constants.ApiPaths;
import com.lokmit.foundation.employment.candidate.entity.Candidate;
import com.lokmit.foundation.employment.candidate.controller.CandidateController;
import com.lokmit.foundation.employment.candidate.repository.CandidateRepository;
import com.lokmit.foundation.employment.candidate.service.CandidateService;
import com.lokmit.foundation.employment.education.controller.CandidateEducationController;
import com.lokmit.foundation.employment.education.entity.CandidateEducation;
import com.lokmit.foundation.employment.education.repository.CandidateEducationRepository;
import com.lokmit.foundation.employment.education.service.CandidateEducationService;
import com.lokmit.foundation.employment.experience.controller.CandidateExperienceController;
import com.lokmit.foundation.employment.experience.entity.CandidateExperience;
import com.lokmit.foundation.employment.experience.repository.CandidateExperienceRepository;
import com.lokmit.foundation.employment.experience.service.CandidateExperienceService;
import com.lokmit.foundation.employment.resume.service.ResumeOwnershipService;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A13 security and CRUD matrix through the REAL Spring Security filter
 * chain (same setup as the A8-A12 matrices). Coverage: anonymous 401 on
 * both CRUD surfaces; ownership-scoped listing; creation persisted for the
 * server-resolved candidate; foreign/unknown record ids masked 404;
 * ownership-scoped PATCH + DELETE; validation failures 400 before any
 * persistence; no-candidate-profile 404; admin surface untouched.
 */
@WebMvcTest(controllers = {CandidateEducationController.class,
        CandidateExperienceController.class, CandidateController.class})
@Import({SecurityConfig.class, CorsConfig.class, JwtKeyConfig.class,
        JwtTokenProvider.class, CustomUserDetailsService.class, SecurityUtils.class,
        CandidateEducationService.class, CandidateExperienceService.class,
        CandidateService.class, ResumeOwnershipService.class})
@AutoConfigureMockMvc
class CandidateEducationExperienceSecurityTest {

    private static final String ME_EDUCATIONS = ApiPaths.CANDIDATE_ME_EDUCATIONS;
    private static final String ME_EXPERIENCES = ApiPaths.CANDIDATE_ME_EXPERIENCES;

    private static final String CANDIDATE_EMAIL = "candidate@lokmitfoundation.org";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CandidateEducationRepository educationRepository;

    @MockitoBean
    private CandidateExperienceRepository experienceRepository;

    @MockitoBean
    private CandidateRepository candidateRepository;

    @MockitoBean
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void resetStubs() {
        reset(educationRepository, experienceRepository,
                candidateRepository, userRepository);
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

    private CandidateEducation education(long id, long candidateId) {
        CandidateEducation e = new CandidateEducation();
        e.setId(id);
        e.setCandidate(candidate(candidateId, dbUser(candidateId, "c" + candidateId + "@x.org")));
        e.setInstitution("Patna Science College");
        e.setDegree("B.Sc.");
        e.setFieldOfStudy("Mathematics");
        e.setStartYear(2015);
        e.setEndYear(2018);
        e.setGrade("First class");
        e.setCreatedAt(OffsetDateTime.now());
        return e;
    }

    private CandidateExperience experience(long id, long candidateId) {
        CandidateExperience e = new CandidateExperience();
        e.setId(id);
        e.setCandidate(candidate(candidateId, dbUser(candidateId, "c" + candidateId + "@x.org")));
        e.setCompanyName("Hope Works");
        e.setJobTitle("Field Coordinator");
        e.setDescription("Managed village outreach");
        e.setStartDate(LocalDate.of(2021, 6, 1));
        e.setEndDate(LocalDate.of(2023, 8, 31));
        e.setCreatedAt(OffsetDateTime.now());
        return e;
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
    // tests
    // ------------------------------------------------------------------

    // ------------------------------------------------------------------
    // authentication + regression
    // ------------------------------------------------------------------

    @Test
    @DisplayName("anonymous access to both CRUD surfaces is rejected 401")
    void anonymousRejected401() throws Exception {
        mockMvc.perform(get(ME_EDUCATIONS)).andExpect(status().isUnauthorized());
        mockMvc.perform(post(ME_EDUCATIONS).contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(patch(ME_EDUCATIONS + "/5").contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete(ME_EDUCATIONS + "/5")).andExpect(status().isUnauthorized());

        mockMvc.perform(get(ME_EXPERIENCES)).andExpect(status().isUnauthorized());
        mockMvc.perform(post(ME_EXPERIENCES).contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(patch(ME_EXPERIENCES + "/5").contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete(ME_EXPERIENCES + "/5")).andExpect(status().isUnauthorized());

        verify(educationRepository, never()).save(any());
        verify(experienceRepository, never()).save(any());
        verify(educationRepository, never()).delete(any());
        verify(experienceRepository, never()).delete(any());
    }

    @Test
    @DisplayName("candidate lists own education + experience records (no candidateId leak)")
    void listOwnRecords() throws Exception {
        stubAuthenticatedCandidate(2L, 10L);
        when(educationRepository.findByCandidateIdOrderByCreatedAtDesc(10L))
                .thenReturn(List.of(education(1L, 10L)));
        when(experienceRepository.findByCandidateIdOrderByCreatedAtDesc(10L))
                .thenReturn(List.of(experience(2L, 10L)));

        mockMvc.perform(get(ME_EDUCATIONS)
                        .header("Authorization", "Bearer " + tokenFor(2L, CANDIDATE_EMAIL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].institution").value("Patna Science College"))
                .andExpect(jsonPath("$.data[0].candidateId").doesNotExist())
                .andExpect(jsonPath("$.data[0].candidate").doesNotExist());

        mockMvc.perform(get(ME_EXPERIENCES)
                        .header("Authorization", "Bearer " + tokenFor(2L, CANDIDATE_EMAIL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].companyName").value("Hope Works"))
                .andExpect(jsonPath("$.data[0].candidateId").doesNotExist())
                .andExpect(jsonPath("$.data[0].candidate").doesNotExist());

        // Ownership-scoped reads only.
        verify(educationRepository).findByCandidateIdOrderByCreatedAtDesc(10L);
        verify(experienceRepository).findByCandidateIdOrderByCreatedAtDesc(10L);
    }

    @Test
    @DisplayName("candidate creates education record (201, persisted for server-resolved candidate)")
    void createEducationRecord() throws Exception {
        stubAuthenticatedCandidate(2L, 10L);
        when(educationRepository.save(any())).thenAnswer(inv -> {
            var e = (CandidateEducation) inv.getArgument(0);
            e.setId(30L);
            return e;
        });

        mockMvc.perform(post(ME_EDUCATIONS)
                        .header("Authorization", "Bearer " + tokenFor(2L, CANDIDATE_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"institution\":\"Patna Science College\","
                                + "\"degree\":\"B.Sc.\",\"startYear\":2015}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(30))
                .andExpect(jsonPath("$.data.candidateId").doesNotExist())
                .andExpect(jsonPath("$.data.candidate").doesNotExist());

        org.mockito.ArgumentCaptor<CandidateEducation> captor =
                org.mockito.ArgumentCaptor.forClass(CandidateEducation.class);
        verify(educationRepository).save(captor.capture());
        assertThat(captor.getValue().getCandidate().getId()).isEqualTo(10L);
        assertThat(captor.getValue().getInstitution()).isEqualTo("Patna Science College");
    }

    @Test
    @DisplayName("candidate creates experience record (201, dates round-trip)")
    void createExperienceRecord() throws Exception {
        stubAuthenticatedCandidate(2L, 10L);
        when(experienceRepository.save(any())).thenAnswer(inv -> {
            var e = (CandidateExperience) inv.getArgument(0);
            e.setId(31L);
            return e;
        });

        mockMvc.perform(post(ME_EXPERIENCES)
                        .header("Authorization", "Bearer " + tokenFor(2L, CANDIDATE_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyName\":\"Hope Works\","
                                + "\"jobTitle\":\"Field Coordinator\","
                                + "\"startDate\":\"2021-06-01\","
                                + "\"endDate\":\"2023-08-31\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(31))
                .andExpect(jsonPath("$.data.startDate").value("2021-06-01"))
                .andExpect(jsonPath("$.data.endDate").value("2023-08-31"))
                .andExpect(jsonPath("$.data.candidateId").doesNotExist());

        verify(experienceRepository).save(any());
    }

    @Test
    @DisplayName("PATCH on foreign or unknown education record is the same masked 404")
    void foreignEducationMasked404() throws Exception {
        stubAuthenticatedCandidate(2L, 10L);
        // Foreign record (candidate 99's id 5): ownership-scoped lookup misses.
        when(educationRepository.findByIdAndCandidateId(5L, 10L)).thenReturn(Optional.empty());
        when(educationRepository.findById(5L)).thenReturn(Optional.of(education(5L, 99L)));

        mockMvc.perform(patch(ME_EDUCATIONS + "/5")
                        .header("Authorization", "Bearer " + tokenFor(2L, CANDIDATE_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"degree\":\"M.Sc.\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));

        // Unknown record — identical behavior.
        when(educationRepository.findByIdAndCandidateId(77L, 10L)).thenReturn(Optional.empty());
        when(educationRepository.findById(77L)).thenReturn(Optional.empty());
        mockMvc.perform(patch(ME_EDUCATIONS + "/77")
                        .header("Authorization", "Bearer " + tokenFor(2L, CANDIDATE_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"degree\":\"M.Sc.\"}"))
                .andExpect(status().isNotFound());

        verify(educationRepository, never()).save(any());
    }

    @Test
    @DisplayName("PATCH + DELETE on own experience records succeed; foreign delete masked")
    void experienceCrudOwnership() throws Exception {
        stubAuthenticatedCandidate(2L, 10L);
        when(experienceRepository.findByIdAndCandidateId(4L, 10L))
                .thenReturn(Optional.of(experience(4L, 10L)));
        when(experienceRepository.save(any()))
                .thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(patch(ME_EXPERIENCES + "/4")
                        .header("Authorization", "Bearer " + tokenFor(2L, CANDIDATE_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"jobTitle\":\"Senior Field Coordinator\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.jobTitle").value("Senior Field Coordinator"));

        mockMvc.perform(delete(ME_EXPERIENCES + "/4")
                        .header("Authorization", "Bearer " + tokenFor(2L, CANDIDATE_EMAIL)))
                .andExpect(status().isNoContent());

        // Own record was deleted exactly once so far.
        verify(experienceRepository, times(1)).delete(any());

        // Foreign experience record (candidate 99's) — same masked 404, and
        // NO additional delete may fire for it.
        when(experienceRepository.findByIdAndCandidateId(6L, 10L)).thenReturn(Optional.empty());
        when(experienceRepository.findById(6L)).thenReturn(Optional.of(experience(6L, 99L)));
        mockMvc.perform(delete(ME_EXPERIENCES + "/6")
                        .header("Authorization", "Bearer " + tokenFor(2L, CANDIDATE_EMAIL)))
                .andExpect(status().isNotFound());
        verify(experienceRepository, times(1)).delete(any());
    }

    @Test
    @DisplayName("validation failures return 400 before any persistence")
    void validationRejectsBadRequests() throws Exception {
        stubAuthenticatedCandidate(2L, 10L);

        // Education: missing required degree; out-of-range year.
        mockMvc.perform(post(ME_EDUCATIONS)
                        .header("Authorization", "Bearer " + tokenFor(2L, CANDIDATE_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"institution\":\"X\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post(ME_EDUCATIONS)
                        .header("Authorization", "Bearer " + tokenFor(2L, CANDIDATE_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"institution\":\"X\",\"degree\":\"B.Sc.\",\"startYear\":1900}"))
                .andExpect(status().isBadRequest());

        // Experience: missing required startDate.
        mockMvc.perform(post(ME_EXPERIENCES)
                        .header("Authorization", "Bearer " + tokenFor(2L, CANDIDATE_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyName\":\"X\",\"jobTitle\":\"Y\"}"))
                .andExpect(status().isBadRequest());

        verify(educationRepository, never()).save(any());
        verify(experienceRepository, never()).save(any());
    }

    @Test
    @DisplayName("a user without a candidate profile receives a plain 404 on both surfaces")
    void nonCandidateProfileGets404() throws Exception {
        User user = dbUser(2L, CANDIDATE_EMAIL);
        when(userRepository.findByEmail(CANDIDATE_EMAIL)).thenReturn(Optional.of(user));
        when(candidateRepository.findByUserId(2L)).thenReturn(Optional.empty());

        mockMvc.perform(get(ME_EDUCATIONS)
                        .header("Authorization", "Bearer " + tokenFor(2L, CANDIDATE_EMAIL)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get(ME_EXPERIENCES)
                        .header("Authorization", "Bearer " + tokenFor(2L, CANDIDATE_EMAIL)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("admin candidate surface remains untouched (anonymous 401; candidate 403)")
    void adminRegressionSmoke() throws Exception {
        mockMvc.perform(get(ApiPaths.ADMIN_CANDIDATES))
                .andExpect(status().isUnauthorized());

        stubAuthenticatedCandidate(2L, 10L);
        mockMvc.perform(get(ApiPaths.ADMIN_CANDIDATES)
                        .header("Authorization", "Bearer " + tokenFor(2L, CANDIDATE_EMAIL)))
                .andExpect(status().isForbidden());
    }
}
