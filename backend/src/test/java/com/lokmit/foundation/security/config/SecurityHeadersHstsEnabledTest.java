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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Production-style HSTS variant of the I-5 security headers, in its own
 * context because {@code @TestPropertySource} values are captured at context
 * startup.
 *
 * <p>Proves the {@code app.security.headers.hsts-*} properties actually drive
 * the emitted {@code Strict-Transport-Security} header — including the
 * {@code includeSubDomains} toggle and a non-default max-age — rather than
 * the configuration being dead code.</p>
 */
@WebMvcTest(controllers = ContactMessageController.class)
@Import({SecurityConfig.class, CorsConfig.class})
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "app.security.headers.hsts-enabled=true",
        "app.security.headers.hsts-max-age-seconds=63072000",
        "app.security.headers.hsts-include-subdomains=false"
})
class SecurityHeadersHstsEnabledTest {

    @Autowired
    private MockMvc mockMvc;

    // Required by the JwtAuthenticationFilter bean declared in SecurityConfig.
    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private ContactMessageService contactMessageService;

    @Test
    @DisplayName("configured HSTS is emitted on secure requests with max-age, without subdomains")
    void hstsEmittedWithConfiguredValues() throws Exception {
        mockMvc.perform(get(ApiPaths.CONTACT_MESSAGES).secure(true))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("Strict-Transport-Security",
                        "max-age=63072000"));
    }

    @Test
    @DisplayName("HSTS is intentionally withheld on plain-HTTP requests (writer semantics)")
    void hstsWithheldOnInsecureRequests() throws Exception {
        mockMvc.perform(get(ApiPaths.CONTACT_MESSAGES))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("Strict-Transport-Security"));
    }

    @Test
    @DisplayName("the rest of the hardened header set is unchanged when HSTS is on")
    void staticHeadersUnchanged() throws Exception {
        mockMvc.perform(get(ApiPaths.CONTACT_MESSAGES))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Referrer-Policy", "no-referrer"));
    }
}
