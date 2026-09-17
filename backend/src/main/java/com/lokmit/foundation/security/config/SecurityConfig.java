package com.lokmit.foundation.security.config;

import com.lokmit.foundation.common.constants.ApiPaths;
import com.lokmit.foundation.common.api.ErrorCodes;
import com.lokmit.foundation.common.api.ApiResponse;
import com.lokmit.foundation.common.api.ApiError;
import com.lokmit.foundation.security.filter.JwtAuthenticationFilter;
import com.lokmit.foundation.security.ratelimit.RateLimitFilter;
import com.lokmit.foundation.security.service.CustomUserDetailsService;
import com.lokmit.foundation.security.service.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.http.HttpMethod;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security configuration for the LOKMIT FOUNDATION API.
 * Configures stateless JWT authentication and role-based authorization.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties({JwtConfig.class, SecurityHeadersProperties.class, RateLimitProperties.class})
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;
    private final SecurityHeadersProperties securityHeadersProperties;

    // NOTE: /api/v1/contact-messages is deliberately NOT listed here as a
    // blanket public path — only its POST method is public (see the method-
    // specific matchers below) so the management endpoints cannot be reached
    // anonymously. First matching rule wins in the authorize chain.
    private static final String[] PUBLIC_ENDPOINTS = {
            ApiPaths.HEALTH,
            ApiPaths.API_V1 + "/auth/login",
            ApiPaths.API_V1 + "/auth/refresh",
            "/api/v1/api-docs",
            "/api/v1/api-docs/**",
            "/api/v1/swagger-ui.html",
            "/api/v1/swagger-ui/**",
            "/swagger-resources/**",
            "/v3/api-docs/**"
    };

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          ObjectMapper objectMapper,
                          SecurityHeadersProperties securityHeadersProperties) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.objectMapper = objectMapper;
        this.securityHeadersProperties = securityHeadersProperties;
    }

    /**
     * Explicitly constructs the JWT authentication filter here instead of
     * annotating the filter class with {@code @Component}. As a plain
     * {@code @Component} implementing {@code Filter}, it would be picked up by
     * {@code @WebMvcTest} slices that cannot supply its service-layer
     * dependencies, breaking those test contexts.
     *
     * <p>Declared {@code static} because this configuration class
     * constructor-injects the filter bean; a static {@code @Bean} method can
     * be invoked without an instance of this class, avoiding a bean-creation
     * cycle.</p>
     */
    @Bean
    public static JwtAuthenticationFilter jwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider,
                                                                  CustomUserDetailsService userDetailsService) {
        return new JwtAuthenticationFilter(jwtTokenProvider, userDetailsService);
    }

    /**
     * I-6 request rate limiting for the public login and contact endpoints.
     * Declared {@code static} for the same bean-cycle reason as
     * {@link #jwtAuthenticationFilter} above.
     */
    @Bean
    public static RateLimitFilter rateLimitFilter(RateLimitProperties rateLimitProperties,
                                                  ObjectMapper objectMapper) {
        return new RateLimitFilter(rateLimitProperties, objectMapper);
    }

    /**
     * Prevents Spring Boot from ALSO auto-registering the rate-limit filter as
     * a top-level servlet filter. It must run only inside the security filter
     * chain (after authentication) so that 429 responses pass through the
     * security headers writer and CORS handling stays authoritative.
     */
    @Bean
    public static FilterRegistrationBean<RateLimitFilter> rateLimitFilterRegistration(RateLimitFilter rateLimitFilter) {
        FilterRegistrationBean<RateLimitFilter> registration = new FilterRegistrationBean<>(rateLimitFilter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   CorsConfigurationSource corsConfigurationSource,
                                                   RateLimitFilter rateLimitFilter) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                // Standard Spring Security CORS integration: preflights are
                // handled by the CorsFilter before authorization; actual requests
                // keep their normal authentication/authorization treatment.
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                // Security response headers (I-5). Spring Security's defaults are
                // kept for X-Content-Type-Options: nosniff, the cache-control set,
                // and X-Frame-Options: DENY (equivalent to frame-ancestors 'none';
                // modern browsers prefer the CSP directive). The explicitly added
                // writers below cover the policy headers the defaults do not set.
                .headers(headers -> headers
                        // The API serves only JSON + springdoc HTML. The CSP keeps
                        // Swagger UI working (its inline scripts and data: images)
                        // while locking down every other document response.
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'none'; frame-ancestors 'none'; base-uri 'none'; "
                                        + "form-action 'none'; img-src 'self' data:; style-src 'self' 'unsafe-inline'; "
                                        + "script-src 'self' 'unsafe-inline'"))
                        .referrerPolicy(referrer -> referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                        // Minimal, API-appropriate feature/permission policy.
                        .permissionsPolicyHeader(permissions -> permissions.policy(
                                "camera=(), microphone=(), geolocation=(), payment=()"))
                        // HSTS is environment-dependent: off in development, on in
                        // production (APP_HSTS_ENABLED=true via application-prod.yml).
                        .httpStrictTransportSecurity(hsts -> {
                            if (securityHeadersProperties.isHstsEnabled()) {
                                hsts.includeSubDomains(securityHeadersProperties.isHstsIncludeSubdomains())
                                        .maxAgeInSeconds(securityHeadersProperties.getHstsMaxAgeSeconds());
                            } else {
                                hsts.disable();
                            }
                        }))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        // POST /contact-messages stays public for the website form;
                        // GET/PATCH require an authenticated principal so anonymous
                        // callers get 401 (not 403) on the management endpoints. The
                        // messages:manage permission is then enforced by @PreAuthorize.
                        .requestMatchers(HttpMethod.POST, ApiPaths.CONTACT_MESSAGES).permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            ApiResponse<Void> error = ApiResponse.error(
                                    "Authentication required",
                                    ApiError.of(ErrorCodes.UNAUTHORIZED, "Authentication required. Please provide a valid bearer token."));
                            response.getWriter().write(objectMapper.writeValueAsString(error));
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            ApiResponse<Void> error = ApiResponse.error(
                                    "Access denied",
                                    ApiError.of(ErrorCodes.FORBIDDEN, "You do not have permission to access this resource."));
                            response.getWriter().write(objectMapper.writeValueAsString(error));
                        })
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                // I-6: runs after authentication so rate limiting never alters
                // authentication/authorization outcomes; it only counts requests.
                .addFilterAfter(rateLimitFilter, JwtAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
}