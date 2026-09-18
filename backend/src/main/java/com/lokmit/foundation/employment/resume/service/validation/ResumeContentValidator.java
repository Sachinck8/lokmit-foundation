package com.lokmit.foundation.employment.resume.service.validation;

import com.lokmit.foundation.common.exception.BadRequestException;
import com.lokmit.foundation.common.exception.PayloadTooLargeException;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Byte-level resume content validation (A7.6.2). Inspects the ACTUAL
 * uploaded bytes — the client-declared MIME type is never the security
 * decision — and accepts exactly three formats:
 *
 * <ul>
 *   <li><b>PDF</b> — a {@code %PDF-} header within the first 1 KiB AND a
 *       {@code %%EOF} marker within the final 1 KiB (rejects truncated,
 *       renamed or concatenated PDF fragments).</li>
 *   <li><b>DOC</b> — the OLE2 / Compound File Binary signature
 *       ({@code D0 CF 11 E0 A1 B1 1A E1}).</li>
 *   <li><b>DOCX</b> — a valid ZIP archive whose {@code [Content_Types].xml}
 *       declares the WordprocessingML main-document content type AND which
 *       contains {@code word/document.xml}. XLSX/PPTX/arbitrary ZIPs are
 *       rejected because their content-type declarations differ or the
 *       required entries are missing.</li>
 * </ul>
 *
 * <p>ZIP inspection is BOUNDED against hostile archives: a hard cap on
 * entry count, entry-name length, per-entry decompressed bytes and the
 * TOTAL decompressed bytes across the archive. The input itself is
 * already capped at {@link #MAX_FILE_SIZE_BYTES} before inspection, so
 * every resource this validator consumes is bounded. Pure JDK byte/ZIP
 * handling — no Tika, per the locked A7.6 dependency decision.</p>
 *
 * <p>Unknown content is REJECTED, never guessed.</p>
 */
@Component
public class ResumeContentValidator {

    /** Maximum accepted resume size: 5 MiB. */
    public static final int MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024;

    /** Header scan window (the PDF header must be near the start). */
    private static final int HEADER_WINDOW = 1024;

    /** Trailer scan window (PDF {@code %%EOF} must be in the final KiB). */
    private static final int TRAILER_WINDOW = 1024;

    /** OLE2 / Compound File Binary signature (legacy DOC). */
    private static final byte[] OLE2_SIGNATURE = {
            (byte) 0xD0, (byte) 0xCF, (byte) 0x11, (byte) 0xE0,
            (byte) 0xA1, (byte) 0xB1, (byte) 0x1A, (byte) 0xE1
    };

    /** PDF header prefix. */
    private static final byte[] PDF_HEADER = {'%', 'P', 'D', 'F', '-'};

    /** PDF trailer marker. */
    private static final byte[] PDF_EOF = {'%', '%', 'E', 'O', 'F'};

    /** ZIP local file header signature (OOXML containers). */
    private static final byte[] ZIP_SIGNATURE = {'P', 'K', 0x03, 0x04};

    private static final String CONTENT_TYPES_ENTRY = "[Content_Types].xml";
    private static final String WORD_DOCUMENT_ENTRY = "word/document.xml";
    private static final String WORDPROCESSINGML_MAIN_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml";

    /** Bounded ZIP inspection limits (hostile-archive defenses). */
    private static final int MAX_ZIP_ENTRIES = 200;
    private static final int MAX_ENTRY_NAME_LENGTH = 255;
    /** Cap for the {@code [Content_Types].xml} text we actually buffer. */
    private static final int MAX_CONTENT_TYPES_BYTES = 64 * 1024;
    /** Total decompressed bytes this validator will ever stream per file. */
    private static final int MAX_TOTAL_DECOMPRESSED_BYTES = 64 * 1024 * 1024;

    /** The three resume formats accepted by the platform. */
    public enum ResumeFormat { PDF, DOC, DOCX }

    /**
     * Validates the uploaded bytes end-to-end: size, format detection and
     * declared-MIME cross-check. Throws {@link BadRequestException} or
     * {@link PayloadTooLargeException} on any violation.
     *
     * @return the format detected from the actual bytes
     */
    public ResumeFormat validate(byte[] content, String declaredMimeType) {
        if (content == null || content.length == 0) {
            throw new BadRequestException("Resume file is empty");
        }
        if (content.length > MAX_FILE_SIZE_BYTES) {
            throw new PayloadTooLargeException(
                    "Resume file exceeds the maximum size of 5 MiB");
        }

        ResumeFormat format = detectFormat(content);
        verifyDeclaredMimeType(format, declaredMimeType);
        return format;
    }

    /**
     * Detects the actual format from the bytes alone. Unknown content is
     * rejected — never guessed.
     */
    public ResumeFormat detectFormat(byte[] content) {
        if (content == null || content.length == 0) {
            throw new BadRequestException("Resume file is empty");
        }

        if (isPdf(content)) {
            return ResumeFormat.PDF;
        }
        if (startsWith(content, OLE2_SIGNATURE)) {
            return ResumeFormat.DOC;
        }
        if (startsWith(content, ZIP_SIGNATURE)) {
            return detectDocx(content);
        }

        throw new BadRequestException(
                "Unsupported or corrupted file content: not a valid PDF, DOC or DOCX file");
    }

    // ------------------------------------------------------------------
    // PDF
    // ------------------------------------------------------------------

    private boolean isPdf(byte[] content) {
        if (!windowContains(content, 0, HEADER_WINDOW, PDF_HEADER)) {
            return false;
        }
        // %%EOF must appear within the final 1 KiB — rejects truncated files.
        int tailStart = Math.max(0, content.length - TRAILER_WINDOW);
        return windowContains(content, tailStart, content.length - tailStart, PDF_EOF);
    }

    private static boolean windowContains(byte[] content, int from, int length, byte[] needle) {
        int end = Math.min(content.length, Math.max(from, 0) + length);
        int limit = end - needle.length;
        for (int i = Math.max(from, 0); i <= limit; i++) {
            boolean match = true;
            for (int j = 0; j < needle.length; j++) {
                if (content[i + j] != needle[j]) {
                    match = false;
                    break;
                }
            }
            if (match) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------
    // DOCX (OOXML / ZIP) — bounded inspection
    // ------------------------------------------------------------------

    private ResumeFormat detectDocx(byte[] content) {
        boolean contentTypesSeen = false;
        boolean declaresWordprocessingMl = false;
        boolean wordDocumentSeen = false;
        int entryCount = 0;
        long totalDecompressed = 0;

        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(content))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (++entryCount > MAX_ZIP_ENTRIES) {
                    throw new BadRequestException(
                            "Malformed document: archive contains too many entries");
                }
                String name = entry.getName();
                if (name == null || name.isEmpty() || name.length() > MAX_ENTRY_NAME_LENGTH) {
                    throw new BadRequestException(
                            "Malformed document: archive entry name invalid");
                }

                if (CONTENT_TYPES_ENTRY.equals(name)) {
                    contentTypesSeen = true;
                    int[] consumed = {0};
                    String declared = readBoundedEntry(zip, MAX_CONTENT_TYPES_BYTES, consumed);
                    totalDecompressed += consumed[0];
                    declaresWordprocessingMl =
                            declared.contains(WORDPROCESSINGML_MAIN_CONTENT_TYPE);
                    if (contentTypesSeen && declaresWordprocessingMl && wordDocumentSeen) {
                        break; // Everything proven — no need to stream the rest.
                    }
                } else {
                    // Every other entry (including word/document.xml) is only
                    // STREAMED within the budget — presence in the archive is
                    // what matters, never its content.
                    if (WORD_DOCUMENT_ENTRY.equals(name)) {
                        wordDocumentSeen = true;
                    }
                    int[] consumed = {0};
                    consumeBoundedEntry(zip, totalDecompressed, consumed);
                    totalDecompressed += consumed[0];
                    if (contentTypesSeen && declaresWordprocessingMl && wordDocumentSeen) {
                        break;
                    }
                }
                zip.closeEntry();
            }
        } catch (BadRequestException e) {
            throw e;
        } catch (IOException e) {
            throw new BadRequestException(
                    "Malformed document: corrupted or invalid archive");
        }

        if (!contentTypesSeen) {
            throw new BadRequestException(
                    "Malformed document: missing [Content_Types].xml — not a valid DOCX");
        }
        if (!declaresWordprocessingMl) {
            throw new BadRequestException(
                    "Malformed document: not a WordprocessingML document "
                            + "(XLSX/PPTX or arbitrary ZIP rejected)");
        }
        if (!wordDocumentSeen) {
            throw new BadRequestException(
                    "Malformed document: missing word/document.xml — not a valid DOCX");
        }
        return ResumeFormat.DOCX;
    }

    /**
     * Reads at most {@code max} bytes of the current entry as UTF-8 text.
     * Writes each chunk EXACTLY ONCE and aborts when the bound is exceeded.
     */
    private String readBoundedEntry(InputStream in, int max, int[] consumed) throws IOException {
        byte[] buffer = new byte[8192];
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        int n;
        while ((n = in.read(buffer)) != -1) {
            if (out.size() + n > max) {
                throw new BadRequestException(
                        "Malformed document: archive entry oversized");
            }
            out.write(buffer, 0, n);
        }
        consumed[0] = out.size();
        return out.toString(StandardCharsets.UTF_8);
    }

    /**
     * Streams (discards) the current entry within the archive-wide
     * decompression budget. A single entry may not exceed the total budget
     * either — both caps use the same counter so a hostile archive cannot
     * hide unbounded expansion across many small entries.
     */
    private void consumeBoundedEntry(InputStream in, long alreadyConsumed, int[] consumed)
            throws IOException {
        byte[] buffer = new byte[8192];
        long total = alreadyConsumed;
        int n;
        while ((n = in.read(buffer)) != -1) {
            total += n;
            if (total > MAX_TOTAL_DECOMPRESSED_BYTES) {
                throw new BadRequestException(
                        "Malformed document: archive decompressed size exceeds the allowed budget");
            }
        }
        consumed[0] = (int) (total - alreadyConsumed);
    }

    // ------------------------------------------------------------------
    // Declared MIME cross-check (content detection always wins)
    // ------------------------------------------------------------------

    /**
     * Cross-checks the client-declared MIME against the DETECTED format.
     * A contradictory declaration is rejected. Neutral
     * {@code application/octet-stream} — or a missing/blank declaration —
     * is accepted for every format; MIME never overrides content detection.
     */
    private void verifyDeclaredMimeType(ResumeFormat format, String declaredMimeType) {
        if (declaredMimeType == null || declaredMimeType.isBlank()) {
            return; // Neutral — content detection decided.
        }
        String mime = declaredMimeType.trim().toLowerCase();
        if ("application/octet-stream".equals(mime)) {
            return; // Neutral generic type, accepted for every format.
        }

        boolean consistent = switch (format) {
            case PDF -> List.of("application/pdf").contains(mime);
            case DOC -> List.of("application/msword").contains(mime);
            case DOCX -> List.of(
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                    .contains(mime);
        };

        if (!consistent) {
            throw new BadRequestException(
                    "Declared file type does not match the actual file content");
        }
    }

    private static boolean startsWith(byte[] content, byte[] prefix) {
        if (content.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (content[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }
}
