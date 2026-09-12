package com.lokmit.foundation.security.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lokmit.foundation.common.constants.ApiPaths;
import com.lokmit.foundation.contact.controller.ContactMessageController;
import com.lokmit.foundation.contact.dto.ContactMessageResponse;
import com.lokmit.foundation.contact.dto.CreateContactMessageRequest;
import com.lokmit.foundation.contact.service.ContactMessageService;
import com.lokmit.foundation.security.service.CustomUserDetailsService;
import com.lokmit.foundation.security.service.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * CORS + Spring Security integration tests through the REAL filter chain.
 *
 * <p>Proves the I-4 contract end to end: preflights work for allowed origins
 * and fail for others, the Authorization/Content-Type headers are permitted,
 * CORS never makes a request anonymous (401/403 semantics unchanged), the
 * public contact POST stays public, and wildcard/credentialed CORS is not in
 * effect.</p>
 */
@WebMvcTest(controllers = ContactMessageController.class)
@Import({SecurityConfig.class, CorsConfig.class})
@AutoConfigureMockMvc
@TestPropertySource(properties = "app.security.cors.allowed-origins=https://app.example.org")
class ContactCorsIntegrationTest {

    private static final String ALLOWED_ORIGIN = "https://app.example.org";
    private static final String DISALLOWED_ORIGIN = "https://evil.example.net";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ContactMessageService contactMessageService;

    // Required by the JwtAuthenticationFilter bean declared in SecurityConfig.
    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    // ------------------------------------------------------------------
    // Preflight (OPTIONS)
    // ------------------------------------------------------------------

    @Test
    @DisplayName("preflight from an allowed origin succeeds and echoes the origin")
    void preflightFromAllowedOriginSucceeds() throws Exception {
        mockMvc.perform(options(ApiPaths.CONTACT_MESSAGES)
                        .header("Origin", ALLOWED_ORIGIN)
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", ALLOWED_ORIGIN))
                .andExpect(header().string("Access-Control-Allow-Methods",
                        org.hamcrest.Matchers.containsString("POST")));
    }

    @Test
    @DisplayName("preflight from a disallowed origin is rejected")
    void preflightFromDisallowedOriginIsRejected() throws Exception {
        mockMvc.perform(options(ApiPaths.CONTACT_MESSAGES)
                        .header("Origin", DISALLOWED_ORIGIN)
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    @DisplayName("preflight permits the Authorization and Content-Type request headers")
    void preflightPermitsRequiredHeaders() throws Exception {
        mockMvc.perform(options(ApiPaths.CONTACT_MESSAGES + "/1")
                        .header("Origin", ALLOWED_ORIGIN)
                        .header("Access-Control-Request-Method", "PATCH")
                        .header("Access-Control-Request-Headers", "Authorization, Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", ALLOWED_ORIGIN))
                .andExpect(header().string("Access-Control-Allow-Headers",
                        org.hamcrest.Matchers.allOf(
                                org.hamcrest.Matchers.containsString("Authorization"),
                                org.hamcrest.Matchers.containsString("Content-Type"))));
    }

    @Test
    @DisplayName("preflight advertises only the API methods (no DELETE/PUT)")
    void preflightAdvertisesMinimumMethods() throws Exception {
        mockMvc.perform(options(ApiPaths.CONTACT_MESSAGES)
                        .header("Origin", ALLOWED_ORIGIN)
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Methods",
                        org.hamcrest.Matchers.allOf(
                                org.hamcrest.Matchers.containsString("GET"),
                                org.hamcrest.Matchers.containsString("POST"),
                                org.hamcrest.Matchers.containsString("PATCH"),
                                org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("DELETE")))));
    }

    @Test
    @DisplayName("credentialed CORS is not enabled (no allow-credentials header)")
    void credentialedCorsIsNotEnabled() throws Exception {
        mockMvc.perform(options(ApiPaths.CONTACT_MESSAGES)
                        .header("Origin", ALLOWED_ORIGIN)
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Access-Control-Allow-Credentials"));
    }

    // ------------------------------------------------------------------
    // Actual requests keep their security semantics
    // ------------------------------------------------------------------

    @Test
    @DisplayName("CORS never makes a request anonymous: anonymous GET is still 401")
    void anonymousGetIsStillUnauthorized() throws Exception {
        mockMvc.perform(get(ApiPaths.CONTACT_MESSAGES)
                        .header("Origin", ALLOWED_ORIGIN))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("CORS never bypasses authorization: unprivileged user is still 403")
    void unprivilegedUserIsStillForbidden() throws Exception {
        mockMvc.perform(get(ApiPaths.CONTACT_MESSAGES)
                        .header("Origin", ALLOWED_ORIGIN)
                        .with(SecurityMockMvcRequestPostProcessors.user("candidate@example.com").roles("CANDIDATE")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("authorized request from an allowed origin succeeds and echoes the origin")
    void authorizedRequestFromAllowedOriginSucceeds() throws Exception {
        when(contactMessageService.listEnquiries(any(), any(), any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(java.util.List.of()));

        mockMvc.perform(get(ApiPaths.CONTACT_MESSAGES)
                        .header("Origin", ALLOWED_ORIGIN)
                        .with(SecurityMockMvcRequestPostProcessors.user("staff@lokmitfoundation.org").authorities(
                                () -> "ROLE_ADMIN", () -> "messages:manage")))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", ALLOWED_ORIGIN))
                .andExpect(jsonPath("$.success").value(true));
    }

    // ------------------------------------------------------------------
    // Public POST remains public from an allowed origin
    // ------------------------------------------------------------------

    @Test
    @DisplayName("public contact POST remains accessible without authentication from an allowed origin")
    void publicPostRemainsAccessible() throws Exception {
        CreateContactMessageRequest request = new CreateContactMessageRequest();
        request.setName("Ramesh Kumar");
        request.setEmail("ramesh@example.com");
        request.setSubject("Enquiry");
        request.setMessage("Hello, we need assistance.");

        when(contactMessageService.submitEnquiry(any(CreateContactMessageRequest.class)))
                .thenReturn(ContactMessageResponse.builder()
                        .id(3L)
                        .status("NEW")
                        .receivedAt("2026-09-12T10:00:00+05:30")
                        .build());

        mockMvc.perform(post(ApiPaths.CONTACT_MESSAGES)
                        .header("Origin", ALLOWED_ORIGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Access-Control-Allow-Origin", ALLOWED_ORIGIN))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(3));
    }

    @Test
    @DisplayName("server-side clients (no Origin header) are unaffected by CORS")
    void serverSideClientsUnaffected() throws Exception {
        mockMvc.perform(get(ApiPaths.CONTACT_MESSAGES))
                .andExpect(status().isUnauthorized());
    }
}
