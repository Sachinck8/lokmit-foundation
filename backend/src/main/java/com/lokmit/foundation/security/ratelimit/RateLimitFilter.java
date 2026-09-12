package com.lokmit.foundation.security.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lokmit.foundation.common.api.ApiError;
import com.lokmit.foundation.common.api.ApiResponse;
import com.lokmit.foundation.common.api.ErrorCodes;
import com.lokmit.foundation.common.constants.ApiPaths;
import com.lokmit.foundation.security.config.RateLimitProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.function.LongSupplier;

/**
 * Request rate limiting for the public, high-risk endpoints (I-6):
 *
 * <ul>
 *   <li>{@code POST /api/v1/auth/login} — complements the I-2 account
 *       lockout, which protects a single account; this limiter protects the
 *       endpoint itself (CPU/BCrypt work, connection slots) from request
 *       flooding with rotating email addresses.</li>
 *   <li>{@code POST /api/v1/contact-messages} — anti-spam/flood protection
 *       for the public contact form.</li>
 * </ul>
 *
 * <p><strong>Only</strong> these two method+path pairs are limited. Preflight
 * (OPTIONS) requests, the health check, Swagger/OpenAPI, refresh/logout, and
 * every authenticated management endpoint are untouched. The filter is
 * deliberately registered <em>after</em> the JWT authentication filter so it
 * never influences authentication decisions; it only counts requests.</p>
 *
 * <p><strong>Client identity:</strong> the servlet remote address. Forwarded
 * headers ({@code X-Forwarded-For} etc.) are deliberately NOT consulted —
 * the application has no trusted-proxy configuration, so any forwarded value
 * would be attacker-controlled and trivially spoofable. When deployed behind
 * a reverse proxy, enable Spring Boot's {@code server.forward-headers-strategy}
 * with a trusted proxy in front so the remote address is set correctly at the
 * socket level.</p>
 *
 * <p><strong>Rejection:</strong> {@code 429 Too Many Requests} in the standard
 * {@link ApiResponse} envelope with {@code Retry-After} (seconds until the
 * client's window resets). No bucket state or internal detail is exposed.</p>
 */
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger(RateLimitFilter.class);

    static final String RATE_LIMITED_MESSAGE = "Too many requests. Please try again later.";

    private final RateLimitProperties properties;
    private final ObjectMapper objectMapper;
    private volatile FixedWindowRateLimiter loginLimiter;
    private volatile FixedWindowRateLimiter contactLimiter;
    private volatile boolean limitersInitialized;

    public RateLimitFilter(RateLimitProperties properties, ObjectMapper objectMapper) {
        this(properties, objectMapper, System::currentTimeMillis);
    }

    /** Test-visible constructor with an injectable clock (deterministic window-reset tests). */
    RateLimitFilter(RateLimitProperties properties, ObjectMapper objectMapper, LongSupplier clockMillis) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.clockMillis = clockMillis;
    }

    private final LongSupplier clockMillis;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Preflights never consume quota (I-4 CORS compatibility).
        return HttpMethod.OPTIONS.matches(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        FixedWindowRateLimiter limiter = limiterFor(request);
        if (limiter == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientKey = request.getRemoteAddr();
        FixedWindowRateLimiter.Decision decision = limiter.tryConsume(clientKey);
        if (decision.allowed()) {
            filterChain.doFilter(request, response);
            return;
        }

        LOG.warn("Rate limit exceeded for a client on a protected public endpoint");
        response.setStatus(429);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("Retry-After", String.valueOf(decision.retryAfterSeconds()));
        ApiResponse<Void> error = ApiResponse.error(
                RATE_LIMITED_MESSAGE,
                ApiError.of(ErrorCodes.RATE_LIMITED, RATE_LIMITED_MESSAGE));
        response.getWriter().write(objectMapper.writeValueAsString(error));
    }

    /** Returns the limiter for this request, or null when the request is not limited. */
    private FixedWindowRateLimiter limiterFor(HttpServletRequest request) {
        boolean isPost = HttpMethod.POST.matches(request.getMethod());
        if (!isPost) {
            return null;
        }
        if (ApiPaths.AUTH_LOGIN.equals(request.getRequestURI())) {
            return loginLimiter(properties.getLogin(), "login");
        }
        if (ApiPaths.CONTACT_MESSAGES.equals(request.getRequestURI())) {
            return contactLimiter(properties.getContact(), "contact");
        }
        return null;
    }

    /** Lazily builds each limiter from configuration (cheap: only first request). */
    private FixedWindowRateLimiter loginLimiter(RateLimitProperties.EndpointRateLimit policy, String name) {
        if (!policy.isEnabled()) {
            return null;
        }
        FixedWindowRateLimiter limiter = loginLimiter;
        if (limiter == null) {
            synchronized (this) {
                if (loginLimiter == null) {
                    loginLimiter = build(policy);
                }
                limiter = loginLimiter;
            }
        }
        return limiter;
    }

    private FixedWindowRateLimiter contactLimiter(RateLimitProperties.EndpointRateLimit policy, String name) {
        if (!policy.isEnabled()) {
            return null;
        }
        FixedWindowRateLimiter limiter = contactLimiter;
        if (limiter == null) {
            synchronized (this) {
                if (contactLimiter == null) {
                    contactLimiter = build(policy);
                }
                limiter = contactLimiter;
            }
        }
        return limiter;
    }

    private FixedWindowRateLimiter build(RateLimitProperties.EndpointRateLimit policy) {
        return new FixedWindowRateLimiter(
                policy.getCapacity(),
                Duration.ofSeconds(policy.getWindowSeconds()),
                policy.getMaxTrackedKeys(),
                clockMillis);
    }
}
