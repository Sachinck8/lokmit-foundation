package com.lokmit.foundation.security.exception;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Test controller for security exception handling tests.
 */
@RestController
public class SecurityTestController {

    @GetMapping("/api/v1/security-test/auth-failed")
    public void authFailed() {
        throw new AuthenticationFailedException("Invalid email or password");
    }

    @GetMapping("/api/v1/security-test/user-locked")
    public void userLocked() {
        throw new UserAccountException("Account is locked", "LOCKED");
    }

    @GetMapping("/api/v1/security-test/user-suspended")
    public void userSuspended() {
        throw new UserAccountException("Account is suspended", "SUSPENDED");
    }

    @GetMapping("/api/v1/security-test/token-expired")
    public void tokenExpired() {
        throw new TokenException("Refresh token expired");
    }

    @GetMapping("/api/v1/security-test/token-invalid")
    public void tokenInvalid() {
        throw new TokenException("Invalid refresh token");
    }
}