package com.lokmit.foundation.security.service;

import com.lokmit.foundation.security.dto.AuthResponse;
import com.lokmit.foundation.security.dto.LoginRequest;
import com.lokmit.foundation.security.dto.RefreshTokenRequest;
import com.lokmit.foundation.security.entity.RefreshToken;
import com.lokmit.foundation.security.entity.User;
import com.lokmit.foundation.security.exception.AuthenticationFailedException;
import com.lokmit.foundation.security.exception.TokenException;
import com.lokmit.foundation.security.exception.UserAccountException;
import com.lokmit.foundation.security.repository.RefreshTokenRepository;
import com.lokmit.foundation.security.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service handling authentication operations: login, refresh, logout.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /**
     * Authenticates a user with email and password.
     *
     * @param request the login request
     * @return the authentication response with tokens
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail().toLowerCase().trim())
                .orElseThrow(() -> new AuthenticationFailedException("Invalid email or password"));

        validateUserStatus(user);

        if (user.getPasswordHash() == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new AuthenticationFailedException("Invalid email or password");
        }

        user.setLastLoginAt(OffsetDateTime.now());
        userRepository.save(user);

        return generateAuthResponse(user);
    }

    /**
     * Refreshes an access token using a valid refresh token.
     * Implements refresh token rotation.
     */
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String rawToken = request.getRefreshToken();
        String tokenHash = hashToken(rawToken);

        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new TokenException("Invalid refresh token"));

        if (storedToken.isExpired()) {
            storedToken.setRevokedAt(OffsetDateTime.now());
            refreshTokenRepository.save(storedToken);
            throw new TokenException("Refresh token expired");
        }

        if (storedToken.isRevoked()) {
            throw new TokenException("Refresh token revoked");
        }

        User user = userRepository.findById(storedToken.getUserId())
                .orElseThrow(() -> new TokenException("User not found"));

        validateUserStatus(user);

        // Revoke old refresh token (rotation)
        storedToken.setRevokedAt(OffsetDateTime.now());
        refreshTokenRepository.save(storedToken);

        return generateAuthResponse(user);
    }

    /**
     * Revokes a refresh token (logout).
     */
    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }

        String tokenHash = hashToken(refreshToken);
        refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {
            token.setRevokedAt(OffsetDateTime.now());
            refreshTokenRepository.save(token);
        });
    }

    /**
     * Gets the current authenticated user's profile.
     */
    @Transactional(readOnly = true)
    public AuthResponse.UserInfo getCurrentUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthenticationFailedException("User not found"));

        Set<String> roles = user.getRoles().stream()
                .map(role -> "ROLE_" + role.getCode())
                .collect(Collectors.toSet());

        Set<String> permissions = user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(com.lokmit.foundation.security.entity.Permission::getCode)
                .collect(Collectors.toSet());

        return AuthResponse.UserInfo.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .userType(user.getUserType())
                .status(user.getStatus())
                .roles(roles)
                .permissions(permissions)
                .build();
    }

    /**
     * Validates that the user account is active.
     */
    private void validateUserStatus(User user) {
        switch (user.getStatus()) {
            case "LOCKED" -> throw new UserAccountException("Account is locked. Please contact support.", "LOCKED");
            case "SUSPENDED" -> throw new UserAccountException("Account is suspended. Please contact support.", "SUSPENDED");
            case "DELETED" -> throw new AuthenticationFailedException("Invalid email or password");
            case "ACTIVE" -> { /* OK */ }
            default -> throw new UserAccountException("Account status prevents authentication.", user.getStatus());
        }
    }

    private AuthResponse generateAuthResponse(User user) {
        Set<String> roleCodes = user.getRoles().stream()
                .map(com.lokmit.foundation.security.entity.Role::getCode)
                .collect(Collectors.toSet());

        Set<String> permissions = user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(com.lokmit.foundation.security.entity.Permission::getCode)
                .collect(Collectors.toSet());

        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail(), roleCodes.stream().toList());
        String rawRefreshToken = jwtTokenProvider.generateRefreshToken();
        String refreshTokenHash = hashToken(rawRefreshToken);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUserId(user.getId());
        refreshToken.setTokenHash(refreshTokenHash);
        refreshToken.setExpiresAt(OffsetDateTime.now().plusSeconds(jwtTokenProvider.getRefreshTokenExpiration() / 1000));
        refreshToken.setCreatedAt(OffsetDateTime.now());
        refreshTokenRepository.save(refreshToken);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(rawRefreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getAccessTokenExpiration() / 1000)
                .user(AuthResponse.UserInfo.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .fullName(user.getFullName())
                        .userType(user.getUserType())
                        .status(user.getStatus())
                        .roles(roleCodes.stream().map("ROLE_"::concat).collect(Collectors.toSet()))
                        .permissions(permissions)
                        .build())
                .build();
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to hash token", e);
        }
    }
}