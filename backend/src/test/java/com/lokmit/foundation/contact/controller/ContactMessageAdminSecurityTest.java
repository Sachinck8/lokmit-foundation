package com.lokmit.foundation.contact.controller;

import com.lokmit.foundation.common.constants.ApiPaths;
import com.lokmit.foundation.contact.dto.ContactMessageAdminResponse;
import com.lokmit.foundation.contact.dto.ContactMessageResponse;
import com.lokmit.foundation.contact.dto.CreateContactMessageRequest;
import com.lokmit.foundation.contact.service.ContactMessageService;
import com.lokmit.foundation.security.config.SecurityConfig;
import com.lokmit.foundation.security.service.CustomUserDetailsService;
import com.lokmit.foundation.security.service.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Authorization matrix for the contact enquiry management API, exercised
 * through the REAL Spring Security filter chain (SecurityConfig is imported
 * and filters are active, unlike the web-slice default).
 *
 * <p>Expected behavior:</p>
 * <ul>
 *   <li>anonymous management access → 401 (authentication entry point)</li>
 *   <li>authenticated without {@code messages:manage} → 403 (method security)</li>
 *   <li>authenticated with {@code messages:manage} → allowed</li>
 *   <li>public POST stays open for the website contact form</li>
 * </ul>
 */
@WebMvcTest(controllers = ContactMessageController.class)
@Import(SecurityConfig.class)
@AutoConfigureMockMvc
class ContactMessageAdminSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ContactMessageService contactMessageService;

    // Required by JwtAuthenticationFilter (declared as a bean in SecurityConfig).
    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private ContactMessageAdminResponse sampleEnquiry() {
        return ContactMessageAdminResponse.builder()
                .id(1L)
                .senderName("Ramesh Kumar")
                .senderEmail("ramesh@example.com")
                .subject("Skill development enquiry")
                .message("We would like to discuss a programme.")
                .status("NEW")
                .internalNote("Enquiry category: consultancy")
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
    }

    // ------------------------------------------------------------------
    // LIST — authorization matrix
    // ------------------------------------------------------------------

    @Test
    @DisplayName("anonymous GET list is rejected with 401")
    void anonymousListIsUnauthorized() throws Exception {
        mockMvc.perform(get(ApiPaths.CONTACT_MESSAGES))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("authenticated user without messages:manage gets 403 on list")
    void authenticatedWithoutPermissionGetsForbidden() throws Exception {
        mockMvc.perform(get(ApiPaths.CONTACT_MESSAGES)
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                                .user("candidate@example.com").roles("CANDIDATE")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("user with messages:manage gets the list")
    void authorizedUserCanList() throws Exception {
        when(contactMessageService.listEnquiries(any(), any(), any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(java.util.List.of()));

        mockMvc.perform(get(ApiPaths.CONTACT_MESSAGES)
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                                .user("admin@lokmitfoundation.org").authorities(
                                () -> "ROLE_ADMIN", () -> "messages:manage")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // ------------------------------------------------------------------
    // SINGLE + UPDATE — authorization matrix
    // ------------------------------------------------------------------

    @Test
    @DisplayName("anonymous GET single is rejected with 401")
    void anonymousGetSingleIsUnauthorized() throws Exception {
        mockMvc.perform(get(ApiPaths.CONTACT_MESSAGES + "/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("authenticated user without permission gets 403 on GET single")
    void noPermissionGetSingleForbidden() throws Exception {
        mockMvc.perform(get(ApiPaths.CONTACT_MESSAGES + "/1")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                                .user("candidate@example.com").roles("CANDIDATE")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("authorized user GET single returns the enquiry DTO")
    void authorizedGetSingleSucceeds() throws Exception {
        when(contactMessageService.getEnquiry(1L)).thenReturn(sampleEnquiry());

        mockMvc.perform(get(ApiPaths.CONTACT_MESSAGES + "/1")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                                .user("staff@lokmitfoundation.org").authorities(
                                () -> "ROLE_MODERATOR", () -> "messages:manage")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.senderEmail").value("ramesh@example.com"))
                .andExpect(jsonPath("$.data.status").value("NEW"));
    }

    @Test
    @DisplayName("anonymous PATCH is rejected with 401")
    void anonymousPatchIsUnauthorized() throws Exception {
        mockMvc.perform(patch(ApiPaths.CONTACT_MESSAGES + "/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"READ\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("authenticated user without permission gets 403 on PATCH")
    void noPermissionPatchForbidden() throws Exception {
        mockMvc.perform(patch(ApiPaths.CONTACT_MESSAGES + "/1")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                                .user("candidate@example.com").roles("CANDIDATE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"READ\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("authorized user PATCH succeeds")
    void authorizedPatchSucceeds() throws Exception {
        when(contactMessageService.updateEnquiry(eq(1L), any()))
                .thenReturn(sampleEnquiry());

        mockMvc.perform(patch(ApiPaths.CONTACT_MESSAGES + "/1")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                                .user("staff@lokmitfoundation.org").authorities(
                                () -> "ROLE_MODERATOR", () -> "messages:manage"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"READ\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // ------------------------------------------------------------------
    // Public POST must remain open (website contact form)
    // ------------------------------------------------------------------

    @Test
    @DisplayName("public POST still works without authentication (website form preserved)")
    void publicPostRemainsOpen() throws Exception {
        CreateContactMessageRequest request = new CreateContactMessageRequest();
        request.setName("Ramesh Kumar");
        request.setEmail("ramesh@example.com");
        request.setSubject("Enquiry");
        request.setMessage("Hello, we need assistance.");

        when(contactMessageService.submitEnquiry(any(CreateContactMessageRequest.class)))
                .thenReturn(ContactMessageResponse.builder()
                        .id(9L)
                        .status("NEW")
                        .receivedAt("2026-09-12T10:00:00+05:30")
                        .build());

        mockMvc.perform(post(ApiPaths.CONTACT_MESSAGES)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(9));
    }

    @Test
    @DisplayName("public POST validation still applies (400 for invalid payload)")
    void publicPostValidationStillApplies() throws Exception {
        CreateContactMessageRequest request = new CreateContactMessageRequest();
        request.setName("");
        request.setEmail("not-an-email");
        request.setMessage("");

        mockMvc.perform(post(ApiPaths.CONTACT_MESSAGES)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------
    // Validation and error mapping through the management endpoints
    // ------------------------------------------------------------------

    @Test
    @DisplayName("invalid status filter is rejected with 400")
    void invalidStatusFilterRejected() throws Exception {
        mockMvc.perform(get(ApiPaths.CONTACT_MESSAGES)
                        .param("status", "HACKED")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                                .user("staff@lokmitfoundation.org").authorities(
                                () -> "ROLE_ADMIN", () -> "messages:manage")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("invalid status in PATCH body is rejected with 400")
    void invalidStatusInPatchRejected() throws Exception {
        mockMvc.perform(patch(ApiPaths.CONTACT_MESSAGES + "/1")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                                .user("staff@lokmitfoundation.org").authorities(
                                () -> "ROLE_ADMIN", () -> "messages:manage"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"NOT_A_STATUS\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("oversized internal note in PATCH body is rejected with 400")
    void oversizedNoteRejected() throws Exception {
        mockMvc.perform(patch(ApiPaths.CONTACT_MESSAGES + "/1")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                                .user("staff@lokmitfoundation.org").authorities(
                                () -> "ROLE_ADMIN", () -> "messages:manage"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                java.util.Map.of("internalNote", "x".repeat(10001)))))
                .andExpect(status().isBadRequest());
    }
}
