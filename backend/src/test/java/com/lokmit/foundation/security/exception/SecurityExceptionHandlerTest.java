package com.lokmit.foundation.security.exception;

import com.lokmit.foundation.common.api.ErrorCodes;
import com.lokmit.foundation.common.exception.GlobalExceptionHandler;
import com.lokmit.foundation.common.exception.TestController;
import com.lokmit.foundation.security.service.CustomUserDetailsService;
import com.lokmit.foundation.security.service.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for the security exception handlers in GlobalExceptionHandler.
 *
 * <p>Uses @AutoConfigureMockMvc(addFilters = false) to bypass the Spring
 * Security filter chain so the test can focus solely on exception mapping
 * without requiring JwtTokenProvider or other security beans.</p>
 */
@WebMvcTest(controllers = SecurityTestController.class)
@AutoConfigureMockMvc(addFilters = false)
class SecurityExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    // JwtAuthenticationFilter extends OncePerRequestFilter (a Filter), which
    // @WebMvcTest picks up automatically. It needs JwtTokenProvider and
    // CustomUserDetailsService, neither of which are @Service beans that
    // @WebMvcTest would scan. Provide mocks to satisfy its constructor.
    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void authenticationFailed_shouldReturn401WithInvalidCredentials() throws Exception {
        mockMvc.perform(get("/api/v1/security-test/auth-failed"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errors[0].code").value(ErrorCodes.INVALID_CREDENTIALS));
    }

    @Test
    void userLocked_shouldReturn401WithUserLocked() throws Exception {
        mockMvc.perform(get("/api/v1/security-test/user-locked"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errors[0].code").value(ErrorCodes.USER_LOCKED));
    }

    @Test
    void userSuspended_shouldReturn401WithUserSuspended() throws Exception {
        mockMvc.perform(get("/api/v1/security-test/user-suspended"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errors[0].code").value(ErrorCodes.USER_SUSPENDED));
    }

    @Test
    void tokenExpired_shouldReturn401WithExpiredToken() throws Exception {
        mockMvc.perform(get("/api/v1/security-test/token-expired"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errors[0].code").value(ErrorCodes.EXPIRED_TOKEN));
    }

    @Test
    void tokenInvalid_shouldReturn401WithInvalidToken() throws Exception {
        mockMvc.perform(get("/api/v1/security-test/token-invalid"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errors[0].code").value(ErrorCodes.INVALID_TOKEN));
    }
}