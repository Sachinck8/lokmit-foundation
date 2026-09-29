package com.lokmit.foundation.admin.user.service;

import com.lokmit.foundation.admin.user.dto.AdminUserResponse;
import com.lokmit.foundation.security.entity.User;

import java.util.stream.Collectors;

/**
 * Maps the {@link User} entity to the safe administrative DTO.
 *
 * <p>Centralizing the mapping here makes it structurally impossible for a
 * security-sensitive column (password hash, brute-force counters, lockout
 * timestamps) to appear in an admin response — the DTO simply has no such
 * fields, and every admin response flows through this single method.</p>
 */
final class AdminUserMapper {

    private AdminUserMapper() {
        throw new AssertionError("Utility class must not be instantiated.");
    }

    static AdminUserResponse toResponse(User user) {
        return AdminUserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .userType(user.getUserType())
                .status(user.getStatus())
                .emailVerified(user.getEmailVerified())
                .roles(user.getRoles().stream()
                        .map(role -> role.getCode())
                        .collect(Collectors.toSet()))
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }
}
