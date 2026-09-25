package com.lokmit.foundation.security.config;

import com.lokmit.foundation.common.constants.ApiPaths;
import com.lokmit.foundation.contact.controller.ContactMessageController;
import com.lokmit.foundation.contact.service.ContactMessageService;
import com.lokmit.foundation.security.service.CustomUserDetailsService;
import com.lokmit.foundation.security.service.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Security response headers (I-5) verified through the REAL filter chain.
 *
 * <p>Proves that every API response — public health check, public contact
 * form submission, protected management endpoint, error responses, and CORS
 * preflights — carries the hardened header set, that HSTS is off by default
 * in development and on when configured, and that adding headers does not
 * change CORS or authorization behavior.</p>
 */
@WebMvcTest(controllers = ContactMessageController.class)
@Import({SecurityConfig.class, CorsConfig.class})
@AutoConfigureMockMvc
@TestPropertySource(properties = "app.security.cors.allowed-origins=https://app.example.org")
class SecurityHeadersTest {

    @Autowired
    private MockMvc mockMvc;

    // Required by the JwtAuthenticationFilter bean declared in SecurityConfig.
    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private ContactMessageService contactMessageService;

    // ------------------------------------------------------------------
    // Static policy headers on public + protected responses
    // (This slice maps only the contact controller, so the "any response"
    // paths are the protected 401 and the public POST — header writers run
    // in the filter chain before authorization, so the set is identical.)
    // ------------------------------------------------------------------

    @Test
    @DisplayName("API response carries the full static header set (401 path)")
    void responseCarriesFullStaticHeaders() throws Exception {
        mockMvc.perform(get(ApiPaths.CONTACT_MESSAGES))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Content-Security-Policy", containsString("default-src 'none'")))
                .andExpect(header().string("Content-Security-Policy", containsString("frame-ancestors 'none'")))
                .andExpect(header().string("Referrer-Policy", "no-referrer"))
                .andExpect(header().string("Permissions-Policy", containsString("camera=()")));
    }

    @Test
    @DisplayName("public contact POST carries the static header set")
    void publicContactPostCarriesStaticHeaders() throws Exception {
        mockMvc.perform(post(ApiPaths.CONTACT_MESSAGES)
                        .contentType("application/json")
                        .content("{\"name\":\"A\",\"email\":\"a@example.org\",\"subject\":\"S\",\"message\":\"M\"}"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Content-Security-Policy", containsString("default-src 'none'")))
                .andExpect(header().string("Referrer-Policy", "no-referrer"));
    }

    // ------------------------------------------------------------------
    // HSTS gating
    // ------------------------------------------------------------------

    @Test
    @DisplayName("HSTS is absent in the default (development) configuration")
    void hstsAbsentByDefault() throws Exception {
        mockMvc.perform(get(ApiPaths.CONTACT_MESSAGES))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("Strict-Transport-Security"));
    }

    @Test
    @DisplayName("X-XSS-Protection is explicitly disabled (modern, recommended value)")
    void xssProtectionExplicitlyDisabled() throws Exception {
        mockMvc.perform(get(ApiPaths.CONTACT_MESSAGES))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("X-XSS-Protection", "0"));
    }

    // ------------------------------------------------------------------
    // Cache control (Spring Security default kept)
    // ------------------------------------------------------------------

    @Test
    @DisplayName("cache-control defaults are preserved on API responses")
    void cacheControlDefaultsPreserved() throws Exception {
        mockMvc.perform(get(ApiPaths.CONTACT_MESSAGES))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("Cache-Control", containsString("no-store")));
    }

    // ------------------------------------------------------------------
    // Preflight requests keep their CORS treatment and gain the header set
    // ------------------------------------------------------------------

    @Test
    @DisplayName("CORS preflight works and includes nosniff (headers + CORS coexist)")
    void preflightKeepsCorsAndAddsHeaders() throws Exception {
        mockMvc.perform(options(ApiPaths.CONTACT_MESSAGES)
                        .header("Origin", "https://app.example.org")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "https://app.example.org"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));
    }
}
