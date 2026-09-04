package com.lokmit.foundation.security.controller;

import com.lokmit.foundation.common.api.ApiResponse;
import com.lokmit.foundation.common.constants.ApiPaths;
import com.lokmit.foundation.security.dto.AuthResponse;
import com.lokmit.foundation.security.dto.LoginRequest;
import com.lokmit.foundation.security.dto.RefreshTokenRequest;
import com.lokmit.foundation.security.service.AuthService;
import com.lokmit.foundation.security.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for authentication endpoints.
 */
@RestController
@RequestMapping(ApiPaths.API_V1 + "/auth")
@Tag(name = "Authentication", description = "Authentication and authorization endpoints")
public class AuthController {

    private final AuthService authService;
    private final SecurityUtils securityUtils;

    public AuthController(AuthService authService, SecurityUtils securityUtils) {
        this.authService = authService;
        this.securityUtils = securityUtils;
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate with email and password", description = "Returns access and refresh tokens on success")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse authResponse = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(authResponse, "Login successful"));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token", description = "Uses a valid refresh token to issue new tokens. The old refresh token is revoked.")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse authResponse = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success(authResponse, "Token refreshed"));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout and revoke refresh token", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success(null, "Logged out successfully"));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user profile", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<AuthResponse.UserInfo>> getCurrentUser() {
        Long userId = securityUtils.getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Not authenticated",
                    com.lokmit.foundation.common.api.ApiError.of("UNAUTHORIZED", "Not authenticated")));
        }
        AuthResponse.UserInfo userInfo = authService.getCurrentUser(userId);
        return ResponseEntity.ok(ApiResponse.success(userInfo));
    }
}