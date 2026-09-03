package com.lokmit.foundation.common.exception;

import com.lokmit.foundation.common.api.ErrorCodes;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies the {@link GlobalExceptionHandler} maps framework and domain
 * exceptions to the standard error envelope with stable status codes.
 */
@WebMvcTest(controllers = TestController.class)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void notFoundException_shouldReturn404Envelope() throws Exception {
        mockMvc.perform(get("/api/v1/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errors[0].code").value(ErrorCodes.NOT_FOUND));
    }

    @Test
    void badRequestException_shouldReturn400Envelope() throws Exception {
        mockMvc.perform(get("/api/v1/test/bad-request"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errors[0].code").value(ErrorCodes.BAD_REQUEST));
    }

    @Test
    void conflictException_shouldReturn409Envelope() throws Exception {
        mockMvc.perform(get("/api/v1/test/conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errors[0].code").value(ErrorCodes.CONFLICT));
    }

    @Test
    void validationError_shouldReturn400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/api/v1/test/body")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errors[0].code").value(ErrorCodes.VALIDATION))
                .andExpect(jsonPath("$.errors[0].field").value("name"));
    }

    @Test
    void parameterValidationError_shouldReturn400Envelope() throws Exception {
        mockMvc.perform(get("/api/v1/test/validated-param").param("number", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errors[0].code").value(ErrorCodes.VALIDATION));
    }

    @Test
    void unknownPath_shouldReturn404Envelope() throws Exception {
        mockMvc.perform(get("/api/v1/test/unknown-404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errors[0].code").value(ErrorCodes.NOT_FOUND));
    }

    @Test
    void unsupportedMethod_shouldReturn405Envelope() throws Exception {
        mockMvc.perform(patch("/api/v1/test/not-found"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errors[0].code").value(ErrorCodes.METHOD_NOT_ALLOWED));
    }

    @Test
    void unsupportedMediaType_shouldReturn415Envelope() throws Exception {
        mockMvc.perform(post("/api/v1/test/body")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("plain text"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errors[0].code").value(ErrorCodes.UNSUPPORTED_MEDIA_TYPE));
    }

    @Test
    void malformedBody_shouldReturn400Envelope() throws Exception {
        mockMvc.perform(post("/api/v1/test/body")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errors[0].code").value(ErrorCodes.BAD_REQUEST));
    }

    @Test
    void typeMismatch_shouldReturn400Envelope() throws Exception {
        mockMvc.perform(get("/api/v1/test/number").param("value", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errors[0].code").value(ErrorCodes.BAD_REQUEST));
    }

    @Test
    void missingParameter_shouldReturn400Envelope() throws Exception {
        mockMvc.perform(get("/api/v1/test/missing-param"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errors[0].code").value(ErrorCodes.BAD_REQUEST));
    }

    @Test
    void genericException_shouldReturn500EnvelopeWithoutInternals() throws Exception {
        mockMvc.perform(get("/api/v1/test/generic"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                                .andExpect(jsonPath("$.errors[0].code").value(ErrorCodes.INTERNAL_ERROR))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"));
    }


    @Test
    void errorEnvelope_shouldIncludeTimestamp() throws Exception {
        mockMvc.perform(get("/api/v1/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }
}