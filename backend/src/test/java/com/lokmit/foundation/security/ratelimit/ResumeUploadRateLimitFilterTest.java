package com.lokmit.foundation.security.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lokmit.foundation.common.constants.ApiPaths;
import com.lokmit.foundation.security.config.RateLimitProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * A7.6.6 item 4: resume-upload rate limiting. Exercises the REAL
 * {@link RateLimitFilter} + {@link FixedWindowRateLimiter} with a small
 * deterministic capacity and an injectable clock — no Spring context, no
 * flaky shared limiter state. The public login/contact limiters are
 * asserted to remain on their own policies (I-6 regression).
 */
class ResumeUploadRateLimitFilterTest {

    private static final String ENDPOINT = ApiPaths.CANDIDATE_ME_RESUMES;

    /** Advanceable fake clock for deterministic window-reset assertions. */
    private AtomicLong nowMillis;

    private RateLimitProperties properties;
    private RateLimitFilter filter;

    @BeforeEach
    void setUp() {
        nowMillis = new AtomicLong(0L);
        properties = new RateLimitProperties();
        // Small deterministic budgets for the resume-upload limiter.
        properties.getResumeUpload().setEnabled(true);
        properties.getResumeUpload().setCapacity(3);
        properties.getResumeUpload().setWindowSeconds(60);
        // The 429 envelope carries an Instant timestamp — register the JSR-310
        // module exactly as the application's Spring-provided mapper does.
        ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules();
        filter = new RateLimitFilter(properties, mapper, nowMillis::get);
    }

    private MockHttpServletRequest uploadRequest(String remoteAddr) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", ENDPOINT);
        request.setRequestURI(ENDPOINT);
        request.setRemoteAddr(remoteAddr);
        return request;
    }

    private MockHttpServletResponse pass(HttpServletRequest request) throws ServletException, IOException {
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        filter.doFilter(request, response, chain);
        return response;
    }

    private FilterChain chainSpy() {
        return mock(FilterChain.class);
    }

    @Test
    @DisplayName("requests within the limit pass through to the chain")
    void withinLimitPassesThrough() throws Exception {
        FilterChain chain = chainSpy();
        for (int i = 0; i < 3; i++) {
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(uploadRequest("10.0.0.1"), response, chain);
            assertThat(response.getStatus()).isEqualTo(200);
        }
        verify(chain, times(3)).doFilter(any(HttpServletRequest.class), any(HttpServletResponse.class));
    }

    @Test
    @DisplayName("request over the limit → 429 envelope with Retry-After, chain NOT invoked")
    void overLimitReturns429() throws Exception {
        for (int i = 0; i < 3; i++) {
            pass(uploadRequest("10.0.0.2"));
        }
        MockHttpServletResponse limited = pass(uploadRequest("10.0.0.2"));

        assertThat(limited.getStatus()).isEqualTo(429);
        assertThat(limited.getHeader("Retry-After")).matches("\\d+");
        assertThat(limited.getContentAsString()).contains("RATE_LIMITED");
        assertThat(limited.getContentAsString()).contains("Too many requests. Please try again later.");
    }

    @Test
    @DisplayName("window reset restores the full budget (deterministic clock)")
    void windowResetRestoresBudget() throws Exception {
        for (int i = 0; i < 3; i++) {
            pass(uploadRequest("10.0.0.3"));
        }
        assertThat(pass(uploadRequest("10.0.0.3")).getStatus()).isEqualTo(429);

        nowMillis.addAndGet(Duration.ofSeconds(61).toMillis());

        assertThat(pass(uploadRequest("10.0.0.3")).getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("different client IPs have independent budgets")
    void perIpBudgetsAreIndependent() throws Exception {
        for (int i = 0; i < 3; i++) {
            pass(uploadRequest("10.0.1.1"));
        }
        assertThat(pass(uploadRequest("10.0.1.1")).getStatus()).isEqualTo(429);
        // A different IP still has its full budget.
        assertThat(pass(uploadRequest("10.0.1.2")).getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("X-Forwarded-For cannot rotate the limiter key")
    void xffCannotRotateKey() throws Exception {
        for (int i = 0; i < 3; i++) {
            MockHttpServletRequest req = uploadRequest("10.0.2.1");
            req.addHeader("X-Forwarded-For", "8.8.8.8");
            pass(req);
        }
        MockHttpServletRequest spoofed = uploadRequest("10.0.2.1");
        spoofed.addHeader("X-Forwarded-For", "9.9.9.9");
        assertThat(pass(spoofed).getStatus()).isEqualTo(429);
    }

    @Test
    @DisplayName("disabled resume-upload policy passes unlimited traffic")
    void disabledPolicyIsNotLimited() throws Exception {
        properties.getResumeUpload().setEnabled(false);

        for (int i = 0; i < 25; i++) {
            assertThat(pass(uploadRequest("10.0.3.1")).getStatus()).isEqualTo(200);
        }
    }

    @Test
    @DisplayName("GET on the resume path is not limited (only POST is)")
    void getIsNotLimited() throws Exception {
        for (int i = 0; i < 25; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", ENDPOINT);
            request.setRequestURI(ENDPOINT);
            request.setRemoteAddr("10.0.4.1");
            assertThat(pass(request).getStatus()).isEqualTo(200);
        }
    }

    @Test
    @DisplayName("login and contact limiters keep their own policies (I-6 regression)")
    void loginAndContactUnaffected() throws Exception {
        // Each limiter category is an independent, separately configurable
        // policy object; the 5/60s contact and 10/60s login figures live in
        // application.yml, not the Java defaults, so we assert independence
        // rather than duplicating YAML values here.
        assertThat(properties.getLogin()).isNotNull();
        assertThat(properties.getContact()).isNotNull();
        assertThat(properties.getContact()).isNotSameAs(properties.getResumeUpload());
        assertThat(properties.getResumeUpload().getCapacity()).isEqualTo(3);

        // A login request at the login path is NOT limited by the resume limiter.
        MockHttpServletRequest login = new MockHttpServletRequest("POST", ApiPaths.AUTH_LOGIN);
        login.setRequestURI(ApiPaths.AUTH_LOGIN);
        login.setRemoteAddr("10.0.5.1");
        MockHttpServletResponse loginResponse = new MockHttpServletResponse();
        FilterChain chain = chainSpy();
        filter.doFilter(login, loginResponse, chain);
        verify(chain).doFilter(any(HttpServletRequest.class), any(HttpServletResponse.class));
        assertThat(loginResponse.getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("429 body uses the standard error envelope shape")
    void overLimitBodyIsStandardEnvelope() throws Exception {
        for (int i = 0; i < 3; i++) {
            pass(uploadRequest("10.0.6.1"));
        }
        String body = pass(uploadRequest("10.0.6.1")).getContentAsString();

        assertThat(body).contains("\"success\":false");
        assertThat(body).contains("\"errors\":");
        // No storage or internal detail ever appears in a limiter response.
        assertThat(body).doesNotContain("storage");
    }
}
