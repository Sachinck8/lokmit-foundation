package com.lokmit.foundation.security.config;

import com.lokmit.foundation.common.constants.ApiPaths;
import com.lokmit.foundation.common.api.ErrorCodes;
import com.lokmit.foundation.common.api.ApiResponse;
import com.lokmit.foundation.common.api.ApiError;
import com.lokmit.foundation.security.filter.JwtAuthenticationFilter;
import com.lokmit.foundation.security.service.CustomUserDetailsService;
import com.lokmit.foundation.security.service.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.http.HttpMethod;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
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
@EnableConfigurationProperties(JwtConfig.class)
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;

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

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, ObjectMapper objectMapper) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.objectMapper = objectMapper;
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

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   CorsConfigurationSource corsConfigurationSource) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                // Standard Spring Security CORS integration: preflights are
                // handled by the CorsFilter before authorization; actual requests
                // keep their normal authentication/authorization treatment.
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
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
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

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