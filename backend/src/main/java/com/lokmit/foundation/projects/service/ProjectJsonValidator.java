package com.lokmit.foundation.projects.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lokmit.foundation.common.exception.BadRequestException;

/**
 * Validates that caller-supplied JSON payloads are well-formed before they
 * are handed to the JSONB {@code projects.objectives} column. Rejects
 * malformed JSON with the standard 400 envelope; the database never sees
 * invalid JSON. Mirrors the A4 CMS validator semantics.
 */
final class ProjectJsonValidator {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ProjectJsonValidator() {
        throw new AssertionError("Utility class must not be instantiated.");
    }

    static void requireWellFormedJson(String json) {
        if (json == null) {
            return; // null means "clear the field", handled by the caller
        }
        try {
            MAPPER.readTree(json);
        } catch (Exception ex) {
            throw new BadRequestException("objectives must contain well-formed JSON");
        }
    }
}
