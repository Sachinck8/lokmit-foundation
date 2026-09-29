package com.lokmit.foundation.employment.resume.controller;

import com.lokmit.foundation.common.constants.ApiPaths;
import com.lokmit.foundation.common.pagination.PageParams;
import com.lokmit.foundation.employment.candidate.repository.CandidateRepository;
import com.lokmit.foundation.employment.resume.entity.Resume;
import com.lokmit.foundation.employment.resume.dto.ResumeResponse;
import com.lokmit.foundation.employment.resume.repository.ResumeRepository;
import com.lokmit.foundation.employment.resume.service.AdminResumeService;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A7.6.6 item 1: admin candidate resume listing security matrix —
 * candidates:manage authorization, pagination, metadata-only DTO,
 * candidate existence behavior, through the REAL security filter chain.
 */
@WebMvcTest(controllers = AdminResumeController.class)
@Import({SecurityConfig.class, CorsConfig.class, JwtKeyConfig.class,
        JwtTokenProvider.class, CustomUserDetailsService.class, SecurityUtils.class,
        AdminResumeService.class})
@AutoConfigureMockMvc
class AdminResumeListingSecurityTest {

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

    private static final String ENDPOINT = ApiPaths.ADMIN_CANDIDATES + "/20/resumes";

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

    private Resume resume(long id, long candidateId, boolean active) {
        Resume r = new Resume();
        r.setId(id);
        r.setCandidateId(candidateId);
        r.setFileName("cv-" + id + ".pdf");
        r.setFileType("application/pdf");
        r.setFileSizeBytes(100L);
        r.setActive(active);
        r.setCreatedAt(OffsetDateTime.now());
        r.setChecksumSha256("a".repeat(64));
        r.setStorageKey("resumes/" + id + "/key");
        r.setFileUrl("internal:db-blob");
        return r;
    }

    private String tokenFor(long userId, String email) {
        return jwtTokenProvider.generateAccessToken(userId, email, List.of());
    }

    @Test
    @DisplayName("anonymous → 401")
    void anonymousIs401() throws Exception {
        mockMvc.perform(get(ENDPOINT)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("CANDIDATE role (no permission) → 403")
    void candidateRoleIs403() throws Exception {
        when(userRepository.findByEmail("candidate@test.local")).thenReturn(Optional.of(
                dbUser(2L, "candidate@test.local", role("CANDIDATE"))));
        mockMvc.perform(get(ENDPOINT)
                        .header("Authorization", "Bearer " + tokenFor(2L, "candidate@test.local")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("admin with candidates:manage lists full resume history (active AND inactive), metadata only")
    void adminListsResumeHistory() throws Exception {
        when(userRepository.findByEmail("admin@test.local")).thenReturn(Optional.of(
                dbUser(4L, "admin@test.local", role("ADMIN", "candidates:manage"))));
        when(candidateRepository.existsById(20L)).thenReturn(true);
        when(resumeRepository.findByCandidateIdOrderByCreatedAtDesc(20L, PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(
                        List.of(resume(11L, 20L, false), resume(10L, 20L, true)),
                        PageRequest.of(0, 20), 2));

        mockMvc.perform(get(ENDPOINT)
                        .header("Authorization", "Bearer " + tokenFor(4L, "admin@test.local")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalItems").value(2))
                .andExpect(jsonPath("$.data.items[0].id").value(11))
                .andExpect(jsonPath("$.data.items[0].active").value(false))
                .andExpect(jsonPath("$.data.items[1].active").value(true))
                // Metadata-only: storage internals never leak.
                .andExpect(jsonPath("$.data.items[0].storageKey").doesNotExist())
                .andExpect(jsonPath("$.data.items[0].fileUrl").doesNotExist())
                .andExpect(jsonPath("$.data.items[0].content").doesNotExist());
    }

    @Test
    @DisplayName("unknown candidate → 404; no resume query executed")
    void unknownCandidateIs404() throws Exception {
        when(userRepository.findByEmail("admin@test.local")).thenReturn(Optional.of(
                dbUser(4L, "admin@test.local", role("ADMIN", "candidates:manage"))));
        when(candidateRepository.existsById(99L)).thenReturn(false);

        mockMvc.perform(get(ApiPaths.ADMIN_CANDIDATES + "/99/resumes")
                        .header("Authorization", "Bearer " + tokenFor(4L, "admin@test.local")))
                .andExpect(status().isNotFound());

        verify(resumeRepository, org.mockito.Mockito.never())
                .findByCandidateIdOrderByCreatedAtDesc(org.mockito.ArgumentMatchers.anyLong(),
                        any(Pageable.class));
    }

    @Test
    @DisplayName("list response never contains storageKey or the internal file_url marker")
    void noStorageInternals() throws Exception {
        when(userRepository.findByEmail("admin@test.local")).thenReturn(Optional.of(
                dbUser(4L, "admin@test.local", role("ADMIN", "candidates:manage"))));
        when(candidateRepository.existsById(20L)).thenReturn(true);
        when(resumeRepository.findByCandidateIdOrderByCreatedAtDesc(20L, PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of(resume(10L, 20L, true)),
                        PageRequest.of(0, 20), 1));

        String body = mockMvc.perform(get(ENDPOINT)
                        .header("Authorization", "Bearer " + tokenFor(4L, "admin@test.local")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        org.assertj.core.api.Assertions.assertThat(body)
                .doesNotContain("resumes/10/key")
                .doesNotContain("internal:db-blob");
    }
}
