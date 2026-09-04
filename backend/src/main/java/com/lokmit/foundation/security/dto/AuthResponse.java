package com.lokmit.foundation.security.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.Set;

/**
 * Response payload after successful authentication or token refresh.
 * Contains the access token, refresh token, and safe user information.
 */
@Getter
@Builder
public class AuthResponse {

    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long expiresIn;
    private UserInfo user;

    /**
     * Safe user information - never includes password_hash.
     */
    @Getter
    @Builder
    public static class UserInfo {
        private Long id;
        private String email;
        private String fullName;
        private String userType;
        private String status;
        private Set<String> roles;
        private Set<String> permissions;
    }
}