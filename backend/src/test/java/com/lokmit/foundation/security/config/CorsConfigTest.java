package com.lokmit.foundation.security.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the explicit CORS policy ({@link CorsConfig}).
 *
 * <p>Verifies the contract directly against the produced
 * {@link org.springframework.web.cors.CorsConfigurationSource}: exact-origin
 * allow-list, no credentialed CORS, minimum method/header surface, and
 * startup failure on a wildcard configuration.</p>
 */
class CorsConfigTest {

    private final CorsConfig corsConfig = new CorsConfig();

    private org.springframework.web.cors.CorsConfiguration policyFor(List<String> origins, String requestOrigin) {
        CorsProperties properties = new CorsProperties();
        properties.setAllowedOrigins(origins);
        var source = corsConfig.corsConfigurationSource(properties);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/contact-messages");
        if (requestOrigin != null) {
            request.addHeader("Origin", requestOrigin);
        }
        return source.getCorsConfiguration(request);
    }

    // ------------------------------------------------------------------
    // Origins
    // ------------------------------------------------------------------

    @Test
    @DisplayName("configured origin is registered exactly")
    void configuredOriginIsRegistered() {
        var config = policyFor(List.of("https://app.example.org"), "https://app.example.org");

        assertNotNull(config);
        assertEquals(List.of("https://app.example.org"), config.getAllowedOrigins());
    }

    @Test
    @DisplayName("unconfigured origin is not in the allow-list")
    void unconfiguredOriginIsRejected() {
        var config = policyFor(List.of("https://app.example.org"), "https://evil.example.net");

        assertNotNull(config);
        assertTrue(config.getAllowedOrigins().stream().noneMatch(o -> o.equals("https://evil.example.net")),
                "Unconfigured origins must not appear in the allow-list");
    }

    @Test
    @DisplayName("multiple origins are accepted from the comma-separated list")
    void multipleOriginsAccepted() {
        var config = policyFor(List.of("http://localhost:5173", "https://app.example.org"), null);

        assertNotNull(config);
        assertEquals(2, config.getAllowedOrigins().size());
    }

    @Test
    @DisplayName("whitespace around origins is trimmed")
    void originsAreTrimmed() {
        var config = policyFor(List.of("  http://localhost:5173  ", "https://app.example.org"), null);

        assertNotNull(config);
        assertTrue(config.getAllowedOrigins().stream().allMatch(o -> !o.startsWith(" ") && !o.endsWith(" ")));
    }

    @Test
    @DisplayName("wildcard origin fails fast at startup")
    void wildcardOriginIsRejected() {
        CorsProperties properties = new CorsProperties();
        properties.setAllowedOrigins(List.of("*"));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> corsConfig.corsConfigurationSource(properties));

        assertTrue(ex.getMessage().contains("APP_CORS_ALLOWED_ORIGINS"));
    }

    @Test
    @DisplayName("empty origin list produces an empty (deny-all) policy")
    void emptyOriginListDeniesAll() {
        var config = policyFor(List.of(), "https://app.example.org");

        assertNotNull(config, "Empty policy must still be registered so preflights answer with no allow");
        assertTrue(config.getAllowedOrigins() == null || config.getAllowedOrigins().isEmpty());
    }

    // ------------------------------------------------------------------
    // Credentials
    // ------------------------------------------------------------------

    @Test
    @DisplayName("credentials are never allowed (Bearer-token API, no cookies)")
    void credentialsAreNeverAllowed() {
        var config = policyFor(List.of("https://app.example.org"), "https://app.example.org");

        assertNotNull(config);
        assertFalse(Boolean.TRUE.equals(config.getAllowCredentials()),
                "allowCredentials must not be true");
    }

    // ------------------------------------------------------------------
    // Methods and headers
    // ------------------------------------------------------------------

    @Test
    @DisplayName("only the API's methods are allowed (GET/POST/PATCH/OPTIONS)")
    void onlyApiMethodsAreAllowed() {
        var config = policyFor(List.of("https://app.example.org"), "https://app.example.org");

        assertNotNull(config);
        var methods = config.getAllowedMethods();
        assertEquals(List.of("GET", "POST", "PATCH", "OPTIONS"), methods);
        assertFalse(methods.contains("DELETE"));
        assertFalse(methods.contains("PUT"));
        assertFalse(methods.contains("*"));
    }

    @Test
    @DisplayName("Authorization and Content-Type are the only allowed headers")
    void minimumHeadersAllowed() {
        var config = policyFor(List.of("https://app.example.org"), "https://app.example.org");

        assertNotNull(config);
        assertEquals(List.of("Authorization", "Content-Type"), config.getAllowedHeaders());
    }

    @Test
    @DisplayName("no response headers are exposed to clients")
    void noExposedHeaders() {
        var config = policyFor(List.of("https://app.example.org"), "https://app.example.org");

        assertNotNull(config);
        assertTrue(config.getExposedHeaders() == null || config.getExposedHeaders().isEmpty());
    }

    @Test
    @DisplayName("preflight max age is finite")
    void preflightMaxAgeIsFinite() {
        var config = policyFor(List.of("https://app.example.org"), "https://app.example.org");

        assertNotNull(config);
        assertEquals(3600L, config.getMaxAge());
    }

    @Test
    @DisplayName("request without Origin header still resolves the policy (non-browser safe)")
    void requestWithoutOriginResolvesPolicy() {
        var config = policyFor(List.of("https://app.example.org"), null);

        assertNotNull(config, "Server-to-server calls without Origin must not break");
    }
}
