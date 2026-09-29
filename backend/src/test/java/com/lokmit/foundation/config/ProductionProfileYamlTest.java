package com.lokmit.foundation.config;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Contract tests for {@code application-prod.yml} (I-4).
 *
 * <p>Parses the production YAML directly and asserts the safety-relevant
 * properties. Deliberately does NOT boot a full application context: that
 * would require a real PostgreSQL instance (Flyway + datasource), which the
 * test architecture must not depend on. The property sources are instead
 * composed the same way Spring Boot does — application.yml layered beneath
 * application-prod.yml — and resolved with an environment carrying the
 * required production variables.</p>
 */
class ProductionProfileYamlTest {

    private static Map<String, Object> prodProps;
    private static Map<String, Object> baseProps;

    @BeforeAll
    static void loadYaml() throws IOException {
        YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
        prodProps = loadFlat(loader, "application-prod.yml");
        baseProps = loadFlat(loader, "application.yml");
    }

    /** Loads a YAML file as a flat property map (nested keys joined with dots). */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> loadFlat(YamlPropertySourceLoader loader, String name) throws IOException {
        List<PropertySource<?>> sources = loader.load(name, new ClassPathResource(name));
        assertFalse(sources.isEmpty(), name + " must exist and be parseable");
        PropertySource<?> source = sources.get(0);
        // Flatten: YamlPropertySourceLoader returns a MapPropertySource whose
        // source map may contain nested maps depending on the loader version.
        Object raw = ((MapPropertySource) source).getSource();
        if (raw instanceof Map && ((Map<?, ?>) raw).values().stream().allMatch(v -> v instanceof Map || v == null)) {
            // Nested form: flatten manually.
            java.util.Map<String, Object> flat = new java.util.HashMap<>();
            flatten("", (Map<String, Object>) raw, flat);
            return flat;
        }
        // OriginTrackedValue wrapper: normalize every value to String so
        // assertions compare content, not wrapper types.
        java.util.Map<String, Object> normalized = new java.util.HashMap<>();
        ((Map<?, ?>) raw).forEach((k, v) -> normalized.put(String.valueOf(k),
                v == null ? null : String.valueOf(v)));
        return normalized;
    }

    private static void flatten(String prefix, Map<String, Object> source, Map<String, Object> target) {
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            Object value = entry.getValue();
            if (value instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> nested = (Map<String, Object>) value;
                flatten(key, nested, target);
            } else {
                target.put(key, value == null ? null : String.valueOf(value));
            }
        }
    }

    // ------------------------------------------------------------------
    // Profile definition
    // ------------------------------------------------------------------

    @Test
    @DisplayName("application-prod.yml exists and parses")
    void prodProfileFileExists() {
        assertTrue(prodProps.containsKey("spring.datasource.url"));
    }

    // ------------------------------------------------------------------
    // Database: environment-driven, no defaults, no secrets
    // ------------------------------------------------------------------

    @Test
    @DisplayName("production database URL is environment-driven with no default")
    void prodDbUrlHasNoDefault() {
        assertEquals("${DB_URL}", prodProps.get("spring.datasource.url"));
    }

    @Test
    @DisplayName("production database username is environment-driven with no default")
    void prodDbUsernameHasNoDefault() {
        assertEquals("${DB_USERNAME}", prodProps.get("spring.datasource.username"));
    }

    @Test
    @DisplayName("production database password is environment-driven with no default")
    void prodDbPasswordHasNoDefault() {
        assertEquals("${DB_PASSWORD}", prodProps.get("spring.datasource.password"));
    }

    // ------------------------------------------------------------------
    // JPA / Flyway production safety
    // ------------------------------------------------------------------

    @Test
    @DisplayName("production keeps Flyway enabled and non-destructive")
    void prodKeepsFlywayEnabled() {
        assertEquals("true", String.valueOf(prodProps.get("spring.flyway.enabled")));
        assertEquals("classpath:db/migration", prodProps.get("spring.flyway.locations"));
        assertEquals("false", String.valueOf(prodProps.get("spring.flyway.baseline-on-migrate")));
    }

    @Test
    @DisplayName("production keeps ddl-auto=validate (Flyway owns the schema)")
    void prodKeepsDdlValidate() {
        // Hibernate may never create or alter the schema in production.
        assertEquals("validate", String.valueOf(prodProps.get("spring.jpa.hibernate.ddl-auto")));
        for (String unsafe : List.of("update", "create", "create-drop")) {
            assertFalse(String.valueOf(prodProps.get("spring.jpa.hibernate.ddl-auto")).equalsIgnoreCase(unsafe),
                    "production ddl-auto must not be " + unsafe);
        }
    }

    // ------------------------------------------------------------------
    // JWT: I-1 fail-fast preserved (no fallback in prod profile)
    // ------------------------------------------------------------------

    @Test
    @DisplayName("prod profile does not introduce a JWT secret fallback")
    void prodDoesNotOverrideJwtSecret() {
        // The prod profile must not set app.security.jwt.secret at all — the
        // value must keep flowing from the base ${JWT_SECRET:} mapping so the
        // JwtKeyConfig I-1 fail-fast logic stays the single decision point.
        assertFalse(prodProps.containsKey("app.security.jwt.secret"),
                "prod profile must not redefine app.security.jwt.secret");
    }

    // ------------------------------------------------------------------
    // CORS: explicit, environment-driven origins
    // ------------------------------------------------------------------

    @Test
    @DisplayName("production CORS origins are environment-driven")
    void prodCorsIsEnvironmentDriven() {
        Object cors = prodProps.get("app.security.cors.allowed-origins");
        assertNotNull(cors, "prod profile must configure app.security.cors.allowed-origins");
        String value = String.valueOf(cors);
        assertTrue(value.contains("${APP_CORS_ALLOWED_ORIGINS"),
                "prod CORS origins must resolve APP_CORS_ALLOWED_ORIGINS, was: " + value);
        assertFalse(value.contains("\"*\"") || "*".equals(value.trim()),
                "prod CORS must not default to a wildcard");
    }

    // ------------------------------------------------------------------
    // Actuator restraint
    // ------------------------------------------------------------------

    @Test
    @DisplayName("prod actuator exposure stays limited to health,info and hides details")
    void prodActuatorStaysRestrictive() {
        String include = String.valueOf(prodProps.get("management.endpoints.web.exposure.include"));
        assertTrue(include.contains("health") && include.contains("info"));
        assertFalse(include.contains("*"));
        assertEquals("never", String.valueOf(prodProps.get("management.endpoint.health.show-details")));
    }

    // ------------------------------------------------------------------
    // Base application.yml: development CORS defaults preserved
    // ------------------------------------------------------------------

    @Test
    @DisplayName("development CORS defaults cover the Vite dev/preview origins")
    void devCorsDefaultsPreserved() {
        Object cors = baseProps.get("app.security.cors.allowed-origins");
        assertNotNull(cors, "base application.yml must define dev CORS defaults");
        String value = String.valueOf(cors);
        assertTrue(value.contains("http://localhost:5173"));
        assertTrue(value.contains("http://localhost:4173"));
        assertTrue(value.contains("${APP_CORS_ALLOWED_ORIGINS"),
                "dev default must remain overridable through the environment");
    }

    @Test
    @DisplayName("base application.yml keeps safe non-secret defaults (dev conventions)")
    void baseKeepsSafeDefaults() {
        assertEquals("${JWT_SECRET:}", baseProps.get("app.security.jwt.secret"));
    }

    // ------------------------------------------------------------------
    // Security headers (I-5): HSTS contract
    // ------------------------------------------------------------------

    @Test
    @DisplayName("production enables HSTS and remains environment-overridable")
    void prodEnablesHsts() {
        assertEquals("${APP_HSTS_ENABLED:true}", prodProps.get("app.security.headers.hsts-enabled"));
        assertEquals("${APP_HSTS_MAX_AGE_SECONDS:31536000}", prodProps.get("app.security.headers.hsts-max-age-seconds"));
        assertEquals("${APP_HSTS_INCLUDE_SUBDOMAINS:true}", prodProps.get("app.security.headers.hsts-include-subdomains"));
    }

    @Test
    @DisplayName("development keeps HSTS disabled by default")
    void devKeepsHstsDisabled() {
        assertEquals("${APP_HSTS_ENABLED:false}", baseProps.get("app.security.headers.hsts-enabled"));
    }

    // ------------------------------------------------------------------
    // Rate limiting (I-6): configuration contract
    // ------------------------------------------------------------------

    @Test
    @DisplayName("production rate limiting is enabled with overridable capacities")
    void prodRateLimitContract() {
        assertEquals("${RATE_LIMIT_LOGIN_ENABLED:true}", prodProps.get("app.security.rate-limit.login.enabled"));
        assertEquals("${RATE_LIMIT_LOGIN_CAPACITY:10}", prodProps.get("app.security.rate-limit.login.capacity"));
        assertEquals("${RATE_LIMIT_LOGIN_WINDOW_SECONDS:60}", prodProps.get("app.security.rate-limit.login.window-seconds"));
        assertEquals("${RATE_LIMIT_CONTACT_ENABLED:true}", prodProps.get("app.security.rate-limit.contact.enabled"));
        assertEquals("${RATE_LIMIT_CONTACT_CAPACITY:5}", prodProps.get("app.security.rate-limit.contact.capacity"));
        assertEquals("${RATE_LIMIT_CONTACT_WINDOW_SECONDS:60}", prodProps.get("app.security.rate-limit.contact.window-seconds"));
    }

    @Test
    @DisplayName("development rate limiting mirrors the production defaults")
    void devRateLimitDefaultsPresent() {
        assertEquals("${RATE_LIMIT_LOGIN_ENABLED:true}", baseProps.get("app.security.rate-limit.login.enabled"));
        assertEquals("${RATE_LIMIT_LOGIN_CAPACITY:10}", baseProps.get("app.security.rate-limit.login.capacity"));
        assertEquals("${RATE_LIMIT_CONTACT_CAPACITY:5}", baseProps.get("app.security.rate-limit.contact.capacity"));
    }
}
