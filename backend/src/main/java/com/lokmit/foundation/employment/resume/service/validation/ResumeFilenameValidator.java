package com.lokmit.foundation.employment.resume.service.validation;

import com.lokmit.foundation.common.exception.BadRequestException;
import org.springframework.stereotype.Component;

/**
 * Filename security validation for resume uploads (A7.6.2).
 *
 * <p>The original filename is DISPLAY METADATA only — it is never used as
 * a storage path (the storage key is server-generated). Even so, hostile
 * filenames are rejected outright, NOT silently rewritten: only the safe
 * normalization below (trim + whitespace collapsing) is applied, and any
 * filename carrying traversal, path, control or illegal characters is
 * refused with a validation error.</p>
 *
 * <p>Rejected outright:</p>
 * <ul>
 *   <li>null/blank, or {@code .}/{@code ..}</li>
 *   <li>path traversal ({@code ../}, {@code ..\}, embedded {@code ..})</li>
 *   <li>absolute Unix paths (leading {@code /})</li>
 *   <li>Windows drive-qualified paths ({@code C:\...}, {@code C:/...})</li>
 *   <li>any {@code /} or {@code \} anywhere</li>
 *   <li>control characters (C0 and DEL)</li>
 *   <li>Windows-illegal characters: {@code < > : " | ? *}</li>
 *   <li>length above {@link #MAX_FILENAME_LENGTH} (VARCHAR(255) bound)</li>
 * </ul>
 */
@Component
public class ResumeFilenameValidator {

    /** Hard length bound — matches the resumes.file_name VARCHAR(255) column. */
    public static final int MAX_FILENAME_LENGTH = 255;

    /** Windows-illegal characters (also blocks NTFS alternate data streams via ':'). */
    private static final String ILLEGAL_CHARACTERS = "<>:\"|?*";

    /**
     * Validates and normalizes the original filename.
     *
     * @return the safe normalized filename (trimmed, whitespace collapsed)
     * @throws BadRequestException when the filename is unsafe
     */
    public String validate(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new BadRequestException("Resume filename is required");
        }

        String name = originalFilename.trim();
        if (name.isEmpty()) {
            throw new BadRequestException("Resume filename is required");
        }
        if (name.length() > MAX_FILENAME_LENGTH) {
            throw new BadRequestException(
                    "Resume filename exceeds the maximum length of " + MAX_FILENAME_LENGTH
                            + " characters");
        }
        if (".".equals(name) || "..".equals(name)) {
            throw new BadRequestException("Resume filename is invalid");
        }
        if (name.contains("..")) {
            throw new BadRequestException("Resume filename must not contain path traversal sequences");
        }
        if (name.contains("/") || name.contains("\\")) {
            throw new BadRequestException("Resume filename must not contain path separators");
        }
        if (name.length() >= 2 && name.charAt(1) == ':') {
            throw new BadRequestException("Resume filename must not be a drive-qualified path");
        }
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c < 0x20 || c == 0x7F) {
                throw new BadRequestException("Resume filename must not contain control characters");
            }
            if (ILLEGAL_CHARACTERS.indexOf(c) >= 0) {
                throw new BadRequestException("Resume filename contains illegal characters");
            }
        }

        return name.replaceAll("\\s+", " ");
    }
}
