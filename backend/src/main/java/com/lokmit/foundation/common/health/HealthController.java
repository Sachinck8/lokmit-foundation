package com.lokmit.foundation.common.health;

import com.lokmit.foundation.common.constants.ApiPaths;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Simple, unauthenticated liveness check for the backend.
 *
 * <p>Reference implementation of a thin controller: it delegates to no service
 * yet and returns a stable JSON payload.</p>
 */
@RestController
@RequestMapping(ApiPaths.HEALTH)
public class HealthController {

    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "UP");
        body.put("service", "lokmit-foundation-backend");
        body.put("version", "0.1.0-SNAPSHOT");
        body.put("timestamp", Instant.now());
        return ResponseEntity.ok(body);
    }
}