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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service handling authentication operations: login, refresh, logout.
 */
@Service
public class AuthService {

    private static final Logger LOG = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final LoginLockoutService loginLockoutService;

    public AuthService(UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider,
                       LoginLockoutService loginLockoutService) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.loginLockoutService = loginLockoutService;
    }

    /**
     * Authenticates a user with email and password.
     *
     * @param request the login request
     * @return the authentication response with tokens
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail().toLowerCase().trim();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthenticationFailedException("Invalid email or password"));

        // Temporary brute-force lockout: rejected before any account-specific
        // processing. Same generic failure as wrong credentials — no enumeration,
        // no remaining lockout time revealed.
        if (loginLockoutService.isLocked(user)) {
            throw new AuthenticationFailedException("Invalid email or password");
        }

        validateUserStatus(user);

        if (user.getPasswordHash() == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            loginLockoutService.recordFailedAttempt(email);
            throw new AuthenticationFailedException("Invalid email or password");
        }

        loginLockoutService.resetOnSuccess(user);
        user.setLastLoginAt(OffsetDateTime.now());
        userRepository.save(user);

        // Each successful authentication starts a fresh refresh-token family.
        return generateAuthResponse(user, UUID.randomUUID());
    }

    /**
     * Refreshes an access token using a valid refresh token.
     * Implements refresh token rotation with reuse detection.
     *
     * <p>Rotation: a presented token is atomically claimed (consumed) before
     * any new tokens are issued, so two simultaneous refreshes with the same
     * token cannot both succeed — the database conditional UPDATE guarantees
     * only one caller wins.</p>
     *
     * <p>Reuse detection: presenting a token that was already consumed,
     * revoked, or expired is treated as reuse. Because a consumed token means
     * its replacement may have been stolen, the safest response is to revoke
     * the token's whole family (every still-valid token minted from the same
     * authentication), forcing re-authentication. The API response stays the
     * existing generic refresh-token failure — no token state is revealed.</p>
     */
    @Transactional(noRollbackFor = TokenException.class)
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String rawToken = request.getRefreshToken();
        String tokenHash = hashToken(rawToken);
        OffsetDateTime now = OffsetDateTime.now();

        // Atomic consumption: 1 = this caller won the token. Runs before any
        // other check, so only one of two simultaneous refreshes can win.
        if (refreshTokenRepository.claimValidToken(tokenHash, now) == 0) {
            handleReuseOrInvalidToken(tokenHash);
            throw new TokenException("Invalid refresh token"); // unreachable: handler always throws
        }

        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new TokenException("Invalid refresh token"));

        User user = userRepository.findById(storedToken.getUserId())
                .orElseThrow(() -> new TokenException("User not found"));

        validateUserStatus(user);

        // Legacy rows created before V10 have no family; adopt them into a
        // fresh one so reuse detection still covers their lineage.
        UUID familyId = storedToken.getFamilyId() != null
                ? storedToken.getFamilyId()
                : UUID.randomUUID();

        return generateAuthResponse(user, familyId);
    }

    /**
     * Shared reuse-detection response for consumed, revoked, expired, or
     * unknown refresh tokens: revoke the token's family (if any) so a replayed
     * token cannot coexist with any valid session from the same authentication,
     * then fail with the existing generic TokenException behavior.
     *
     * <p>The surrounding refresh transaction commits on TokenException
     * ({@code noRollbackFor}), so the protective family revocation survives
     * even though the client receives the usual generic refresh failure.
     * Error codes match the previous behavior: expired tokens keep the
     * expired code; consumed/revoked/unknown tokens keep the invalid code.</p>
     */
    private void handleReuseOrInvalidToken(String tokenHash) {
        refreshTokenRepository.findByTokenHash(tokenHash).ifPresentOrElse(token -> {
            if (token.getFamilyId() != null) {
                int revoked = refreshTokenRepository.revokeAllInFamily(token.getFamilyId(), OffsetDateTime.now());
                if (revoked > 0) {
                    LOG.info("Refresh token reuse detected: revoked {} active token(s) in the compromised family", revoked);
                }
            }
            if (token.isExpired()) {
                throw new TokenException("Refresh token expired");
            }
            throw new TokenException("Refresh token reuse detected");
        }, () -> {
            throw new TokenException("Invalid refresh token");
        });
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

    /**
     * Issues a new token pair belonging to the given refresh-token family.
     * Called with a fresh family on login, or with the consumed token's
     * family on refresh (rotation within the same lineage).
     */
    private AuthResponse generateAuthResponse(User user, UUID familyId) {
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
        refreshToken.setFamilyId(familyId);
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