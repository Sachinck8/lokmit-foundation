package com.lokmit.foundation.common.health;

import com.lokmit.foundation.common.constants.ApiPaths;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Guards the Phase 1 health contract so later phases cannot break it silently.
 */
@WebMvcTest(controllers = HealthController.class)
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

        @Test
    void health_shouldReturnUpWithServiceMetadata() throws Exception {
        mockMvc.perform(get(ApiPaths.HEALTH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("UP"))
                .andExpect(jsonPath("$.data.service").value("lokmit-foundation-backend"))
                .andExpect(jsonPath("$.data.version").value("0.1.0-SNAPSHOT"))
                .andExpect(jsonPath("$.data.timestamp").isNotEmpty());
    }
}