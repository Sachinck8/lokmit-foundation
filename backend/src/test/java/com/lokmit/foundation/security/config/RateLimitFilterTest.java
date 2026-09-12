package com.lokmit.foundation.security.config;

import com.lokmit.foundation.common.constants.ApiPaths;
import com.lokmit.foundation.contact.controller.ContactMessageController;
import com.lokmit.foundation.contact.service.ContactMessageService;
import com.lokmit.foundation.security.controller.AuthController;
import com.lokmit.foundation.security.exception.AuthenticationFailedException;
import com.lokmit.foundation.security.exception.TokenException;
import com.lokmit.foundation.security.service.AuthService;
import com.lokmit.foundation.security.service.CustomUserDetailsService;
import com.lokmit.foundation.security.service.JwtTokenProvider;
import com.lokmit.foundation.security.util.SecurityUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * I-6 end-to-end behavior through the REAL security filter chain.
 *
 * <p>Verifies 429 handling (envelope, Retry-After, I-5 security headers),
 * endpoint scoping (only POST login/contact are limited; preflights, other
 * methods, refresh, and protected admin endpoints are untouched), per-IP
 * independence, XFF spoofing immunity, and that validation and the I-2
 * generic-failure flow still run beneath the limiter.</p>
 */
@WebMvcTest(controllers = {ContactMessageController.class, AuthController.class})
@Import({SecurityConfig.class, CorsConfig.class})
@AutoConfigureMockMvc
@TestPropertySource(properties = "app.security.cors.allowed-origins=https://app.example.org")
class RateLimitFilterTest {

    /*
     * All tests in this class share one Spring context (and therefore one
     * RateLimitFilter with live windows), so every test keys its requests to
     * a UNIQUE client IP to keep budgets isolated per test.
     */
    private static final String IP_LOGIN_A = "203.0.113.10";
    private static final String IP_LOGIN_B = "203.0.113.11";
    private static final String IP_LOGIN_C = "203.0.113.12";
    private static final String IP_LOGIN_D = "203.0.113.13";
    private static final String IP_LOGIN_E = "203.0.113.14";
    private static final String IP_LOGIN_F = "203.0.113.15";
    private static final String IP_LOGIN_G = "203.0.113.16";
    private static final String IP_CONTACT_A = "198.51.100.10";
    private static final String IP_CONTACT_B = "198.51.100.11";
    private static final String IP_CONTACT_C = "198.51.100.12";
    private static final String IP_CONTACT_D = "198.51.100.13";

    private static final String VALID_LOGIN_BODY =
            "{\"email\":\"user@example.org\",\"password\":\"whatever\"}";
    private static final String CONTACT_BODY =
            "{\"name\":\"A\",\"email\":\"a@example.org\",\"subject\":\"S\",\"message\":\"M\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ContactMessageService contactMessageService;

    @MockitoBean
    private AuthService authService;

    // Required by the JwtAuthenticationFilter bean declared in SecurityConfig.
    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    // Required by the AuthController constructor.
    @MockitoBean
    private SecurityUtils securityUtils;

    /**
     * Every login request that gets past the rate limiter fails with the
     * generic I-2 credentials error — a deterministic, enumeration-safe proof
     * that the request actually reached authentication processing.
     */
    @BeforeEach
    void stubAuthenticationFailure() {
        Mockito.lenient()
                .doThrow(new AuthenticationFailedException("Invalid email or password"))
                .when(authService).login(Mockito.any());
        Mockito.lenient()
                .doThrow(new TokenException("Invalid refresh token"))
                .when(authService).refreshToken(Mockito.any());
    }

    @AfterEach
    void resetAuthService() {
        Mockito.reset(authService);
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder loginFrom(String ip) {
        return post(ApiPaths.AUTH_LOGIN)
                .with(request -> {
                    request.setRemoteAddr(ip);
                    return request;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_LOGIN_BODY);
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder contactFrom(String ip) {
        return post(ApiPaths.CONTACT_MESSAGES)
                .with(request -> {
                    request.setRemoteAddr(ip);
                    return request;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content(CONTACT_BODY);
    }

    // ------------------------------------------------------------------
    // Login endpoint limiting (default capacity: 10 per 60s window)
    // ------------------------------------------------------------------

    @Test
    @DisplayName("requests under the login limit reach authentication processing (generic 401)")
    void underLimitReachesAuthentication() throws Exception {
        for (int i = 0; i < 8; i++) {
            mockMvc.perform(loginFrom(IP_LOGIN_A))
                    .andExpect(status().isUnauthorized()) // I-2 generic failure path
                    .andExpect(jsonPath("$.errors[0].code").value("INVALID_CREDENTIALS"));
        }
    }

    @Test
    @DisplayName("requests over the login limit return 429 with the standard envelope")
    void overLimitReturns429() throws Exception {
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(loginFrom(IP_LOGIN_B))
                    .andExpect(status().isUnauthorized());
        }
        mockMvc.perform(loginFrom(IP_LOGIN_B))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Too many requests. Please try again later."))
                .andExpect(jsonPath("$.errors[0].code").value("RATE_LIMITED"));
    }

    @Test
    @DisplayName("429 carries Retry-After and the I-5 security headers")
    void rateLimitedResponseHasRetryAfterAndSecurityHeaders() throws Exception {
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(loginFrom(IP_LOGIN_C)).andReturn();
        }
        mockMvc.perform(loginFrom(IP_LOGIN_C))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", org.hamcrest.Matchers.matchesPattern("\\d+")))
                // I-5 headers are written by the security chain wrapping the limiter's 429.
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Content-Security-Policy",
                        org.hamcrest.Matchers.containsString("default-src 'none'")))
                .andExpect(header().string("Referrer-Policy", "no-referrer"));
    }

    @Test
    @DisplayName("different client IPs have independent login limits")
    void differentIpsAreIndependent() throws Exception {
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(loginFrom(IP_LOGIN_D))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.errors[0].code").value("INVALID_CREDENTIALS"));
        }
        // A different IP still has its full budget.
        mockMvc.perform(loginFrom(IP_LOGIN_E))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errors[0].code").value("INVALID_CREDENTIALS"));
    }

    @Test
    @DisplayName("X-Forwarded-For cannot rotate the limiter key (no spoofing vector)")
    void xffHeaderCannotRotateKey() throws Exception {
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(loginFrom(IP_LOGIN_F).header("X-Forwarded-For", "8.8.8.8")) // attacker-supplied, ignored
                    .andReturn();
        }
        // Even with a fresh spoofed XFF, the client is still limited.
        mockMvc.perform(loginFrom(IP_LOGIN_F).header("X-Forwarded-For", "9.9.9.9"))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    @DisplayName("validation failures occur beneath the limiter and consume quota")
    void validationRunsBeneathLimiter() throws Exception {
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(post(ApiPaths.AUTH_LOGIN)
                            .with(request -> {
                                request.setRemoteAddr(IP_LOGIN_G);
                                return request;
                            })
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"not-an-email\",\"password\":\"\"}"))
                    .andExpect(status().isBadRequest());
        }
        mockMvc.perform(post(ApiPaths.AUTH_LOGIN)
                        .with(request -> {
                            request.setRemoteAddr(IP_LOGIN_G);
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\",\"password\":\"\"}"))
                .andExpect(status().isTooManyRequests());
    }

    // ------------------------------------------------------------------
    // Contact endpoint limiting (default capacity: 5 per 60s window)
    // ------------------------------------------------------------------

    @Test
    @DisplayName("contact requests over the limit return 429; under-limit keeps 201")
    void contactOverLimitReturns429() throws Exception {
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(contactFrom(IP_CONTACT_A))
                    .andExpect(status().isCreated());
        }
        mockMvc.perform(contactFrom(IP_CONTACT_A))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.errors[0].code").value("RATE_LIMITED"));
    }

    @Test
    @DisplayName("different client IPs have independent contact limits")
    void contactIpsAreIndependent() throws Exception {
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(contactFrom(IP_CONTACT_B))
                    .andExpect(status().isCreated());
        }
        mockMvc.perform(contactFrom(IP_CONTACT_C))
                .andExpect(status().isCreated());
    }

    // ------------------------------------------------------------------
    // Scoping: what must NOT be limited
    // ------------------------------------------------------------------

    @Test
    @DisplayName("CORS preflights do not consume quota")
    void preflightDoesNotConsumeQuota() throws Exception {
        mockMvc.perform(options(ApiPaths.CONTACT_MESSAGES)
                        .header("Origin", "https://app.example.org")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk());
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(contactFrom(IP_CONTACT_D))
                    .andExpect(status().isCreated());
        }
        // Even though POSTs are now limited, preflights still succeed.
        mockMvc.perform(options(ApiPaths.CONTACT_MESSAGES)
                        .header("Origin", "https://app.example.org")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET on the contact path is not limited (protected, auth-only path)")
    void getOnContactPathIsNotLimited() throws Exception {
        for (int i = 0; i < 20; i++) {
            mockMvc.perform(get(ApiPaths.CONTACT_MESSAGES))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Test
    @DisplayName("refresh endpoint is not rate limited")
    void refreshIsNotLimited() throws Exception {
        for (int i = 0; i < 20; i++) {
            mockMvc.perform(post(ApiPaths.API_V1 + "/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"refreshToken\":\"some-token\"}"))
                    .andExpect(status().isUnauthorized());
        }
    }
}
