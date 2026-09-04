package com.lokmit.foundation.security.util;

import com.lokmit.foundation.security.service.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

/**
 * Utility class for accessing security context information.
 */
@Component
public class SecurityUtils {

    private static final Logger LOG = LoggerFactory.getLogger(SecurityUtils.class);

    private final JwtTokenProvider jwtTokenProvider;

    public SecurityUtils(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /**
     * Extracts the current user ID from the security context.
     *
     * @return the user ID, or null if not authenticated
     */
    public Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails) {
            Object credentials = authentication.getCredentials();
            if (credentials instanceof String token && !token.isEmpty()) {
                return jwtTokenProvider.extractUserId(token);
            }
        }

        return null;
    }

    /**
     * Extracts the current user's email from the security context.
     *
     * @return the email, or null if not authenticated
     */
    public String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }

        return null;
    }
}