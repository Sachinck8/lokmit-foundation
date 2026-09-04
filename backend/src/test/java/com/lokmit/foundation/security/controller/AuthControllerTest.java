package com.lokmit.foundation.security.controller;

import com.lokmit.foundation.common.constants.ApiPaths;
import com.lokmit.foundation.security.dto.LoginRequest;
import com.lokmit.foundation.security.dto.RefreshTokenRequest;
import com.lokmit.foundation.security.service.AuthService;
import com.lokmit.foundation.security.service.JwtTokenProvider;
import com.lokmit.foundation.security.util.SecurityUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.bean.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private SecurityUtils securityUtils;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void login_shouldReturn200WithTokens() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("admin@lokmitfoundation.org");
        request.setPassword("SecureP@ss123");

        var authResponse = com.lokmit.foundation.security.dto.AuthResponse.builder()
                .accessToken("test-access-token")
                .refreshToken("test-refresh-token")
                .tokenType("Bearer")
                .expiresIn(900L)
                .build();

        when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post(ApiPaths.AUTH_LOGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("test-access-token"))
                .andExpect(jsonPath("$.data.refreshToken").value("test-refresh-token"))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"));
    }

    @Test
    void login_shouldReturn400ForInvalidEmail() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("invalid-email");
        request.setPassword("SecureP@ss123");

        mockMvc.perform(post(ApiPaths.AUTH_LOGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void login_shouldReturn400ForBlankPassword() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("admin@lokmitfoundation.org");
        request.setPassword("");

        mockMvc.perform(post(ApiPaths.AUTH_LOGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void refresh_shouldReturn200WithNewTokens() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("valid-refresh-token");

        var authResponse = com.lokmit.foundation.security.dto.AuthResponse.builder()
                .accessToken("new-access-token")
                .refreshToken("new-refresh-token")
                .tokenType("Bearer")
                .expiresIn(900L)
                .build();

        when(authService.refreshToken(any(RefreshTokenRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post(ApiPaths.AUTH_REFRESH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("new-access-token"));
    }

    @Test
    void refresh_shouldReturn400ForBlankToken() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("");

        mockMvc.perform(post(ApiPaths.AUTH_REFRESH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void logout_shouldReturn200() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("valid-refresh-token");

        mockMvc.perform(post(ApiPaths.AUTH_LOGOUT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void me_shouldReturn401WithoutAuth() throws Exception {
        mockMvc.perform(get(ApiPaths.AUTH_ME))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "admin@lokmitfoundation.org")
    void me_shouldReturnUserProfile() throws Exception {
        var userInfo = com.lokmit.foundation.security.dto.AuthResponse.UserInfo.builder()
                .id(1L)
                .email("admin@lokmitfoundation.org")
                .fullName("Platform Administrator")
                .userType("STAFF")
                .status("ACTIVE")
                .build();

        when(securityUtils.getCurrentUserId()).thenReturn(1L);
        when(authService.getCurrentUser(1L)).thenReturn(userInfo);

        mockMvc.perform(get(ApiPaths.AUTH_ME))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("admin@lokmitfoundation.org"))
                .andExpect(jsonPath("$.data.fullName").value("Platform Administrator"));
    }
}