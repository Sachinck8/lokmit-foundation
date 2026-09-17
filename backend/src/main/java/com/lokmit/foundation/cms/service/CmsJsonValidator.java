package com.lokmit.foundation.cms.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lokmit.foundation.common.exception.BadRequestException;

/**
 * Validates that caller-supplied JSON payloads are well-formed before they
 * are handed to the JSONB column. Rejects malformed JSON with the standard
 * 400 envelope; the database never sees invalid JSON.
 */
final class CmsJsonValidator {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private CmsJsonValidator() {
        throw new AssertionError("Utility class must not be instantiated.");
    }

    static void requireWellFormedJson(String json, String fieldName) {
        if (json == null) {
            return; // null means "clear the field", handled by the caller
        }
        try {
            MAPPER.readTree(json);
        } catch (Exception ex) {
            throw new BadRequestException(fieldName + " must contain well-formed JSON");
        }
    }
}
