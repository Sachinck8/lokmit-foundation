package com.lokmit.foundation.security.filter;

import com.lokmit.foundation.security.service.CustomUserDetailsService;
import com.lokmit.foundation.security.service.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter that validates JWT tokens from the Authorization header and sets up Spring Security context.
 * Expects the header format: "Bearer <token>"
 *
 * <p>Deliberately NOT annotated with {@code @Component}: it is registered as a
 * {@code @Bean} in {@code SecurityConfig} so that web-layer test slices
 * ({@code @WebMvcTest}) do not instantiate it and its service-layer
 * dependencies (JwtTokenProvider, CustomUserDetailsService).</p>
 *
 * <p><strong>Database-backed status enforcement (A1):</strong> on every
 * request the user is reloaded from the database via
 * {@link CustomUserDetailsService}. A principal whose account is no longer
 * {@code ACTIVE} (locked, suspended, or deleted) is treated as disabled and
 * is <em>not</em> authenticated — a still-unexpired access JWT therefore
 * cannot keep a deactivated account working. Roles and permissions are also
 * rebuilt from the database here, so JWT {@code roles} claims are never the
 * source of authorization decisions.</p>
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, CustomUserDetailsService userDetailsService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        try {
            authenticateRequest(request);
            filterChain.doFilter(request, response);
        } finally {
            // Stateless API: never leak authentication state between requests
            // running on reused container threads.
            SecurityContextHolder.clearContext();
        }
    }

    /**
     * Validates the bearer token and populates the security context when the
     * token is valid AND the database-backed account is enabled. Any failure
     * leaves the context empty so downstream rules treat the request as
     * anonymous (401 for protected endpoints via the authentication entry
     * point).
     */
    private void authenticateRequest(HttpServletRequest request) {
        String token = extractToken(request);

        if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
            Claims claims = jwtTokenProvider.extractClaims(token);
            if (claims != null) {
                String email = claims.get("email", String.class);
                if (StringUtils.hasText(email)) {
                    try {
                        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
                        // Fail closed on disabled accounts (A1): a user whose
                        // status is no longer ACTIVE (locked, suspended, or
                        // deleted by an administrator) must not keep a working
                        // session just because their access JWT is still
                        // unexpired.
                        if (!userDetails.isEnabled()) {
                            LOG.debug("Rejecting authentication for non-ACTIVE account");
                            return;
                        }
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        userDetails, token, userDetails.getAuthorities());
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    } catch (Exception ex) {
                        LOG.debug("Failed to authenticate user from JWT: {}", ex.getMessage());
                    }
                }
            }
        }
    }

    /**
     * Extracts the JWT token from the Authorization header.
     *
     * @param request the HTTP request
     * @return the token, or null if not present or not a Bearer token
     */
    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
