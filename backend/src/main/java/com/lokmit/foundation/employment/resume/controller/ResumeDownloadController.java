package com.lokmit.foundation.employment.resume.controller;

import com.lokmit.foundation.common.constants.ApiPaths;
import com.lokmit.foundation.employment.resume.entity.Resume;
import com.lokmit.foundation.employment.resume.service.ResumeDownloadService;
import com.lokmit.foundation.security.Permissions;
import com.lokmit.foundation.security.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

/**
 * Secure resume download API (A7.6.4).
 *
 * <p>Thin HTTP surface over {@link ResumeDownloadService}: authorization,
 * the active rule, storage access and integrity verification all live in
 * the service; this controller only shapes the HTTP response. The storage
 * key is resolved server-side from authorized metadata — it is never a
 * client input and never appears in any response, header, or log.</p>
 *
 * <p>Authorization: candidate OWNERSHIP (own ACTIVE resume only) or the
 * existing {@code candidates:manage} employment permission (SUPER_ADMIN +
 * ADMIN, V14 seed). Employers/clients/moderators get 403/404, never
 * bytes. The candidate/user id is never accepted from the request.</p>
 *
 * <p>Responses are binary ({@code application/pdf} / {@code msword} /
 * {@code wordprocessingml.document} derived from validated upload
 * metadata, never a client header), served as an attachment with a
 * header-injection-safe filename and {@code Cache-Control: no-store} —
 * resume bytes are never publicly cacheable.</p>
 */
@RestController
@Tag(name = "Resume Download",
        description = "Secure resume download (candidate ownership or candidates:manage)")
public class ResumeDownloadController {

    private final ResumeDownloadService downloadService;
    private final SecurityUtils securityUtils;

    public ResumeDownloadController(ResumeDownloadService downloadService,
                                    SecurityUtils securityUtils) {
        this.downloadService = downloadService;
        this.securityUtils = securityUtils;
    }

    @GetMapping(ApiPaths.RESUME_DOWNLOAD)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Download a resume by id",
            description = "Candidates may download ONLY their own ACTIVE resume. "
                    + "Administrators with candidates:manage may download any resume. "
                    + "Binary response, no-store caching, storage internals never exposed.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<byte[]> download(@PathVariable long resumeId) {
        Long userId = securityUtils.getCurrentUserId();
        boolean isAdmin = securityUtils.hasAuthority(Permissions.CANDIDATES_MANAGE);

        ResumeDownloadService.Download download =
                downloadService.loadForPrincipal(resumeId, userId, isAdmin);
        Resume resume = download.resume();

        return ResponseEntity.status(HttpStatus.OK)
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition(resume.getFileName()))
                .contentType(mediaTypeFor(resume.getFileType()))
                .body(download.content());
    }

    /**
     * RFC 5987/6266-safe attachment filename: the filename passed A7.6.2
     * validation at upload (no control characters, no path separators, no
     * quotes), and it is additionally percent-encoded here as UTF-8 BYTES
     * (the RFC 5987 ext-value encoding) so a hostile stored name can never
     * inject or fold response headers. A parallel ASCII fallback
     * ({@code filename}) keeps older clients working.
     */
    static String contentDisposition(String validatedFileName) {
        String asciiFallback = validatedFileName.replaceAll("[^\\x20-\\x7E]", "_")
                .replace("\"", "_");
        StringBuilder encoded = new StringBuilder();
        for (byte b : validatedFileName.getBytes(StandardCharsets.UTF_8)) {
            int v = b & 0xFF;
            // attr-char (RFC 5987 §3.2.1): visible ASCII minus *%' and quote
            if (v >= 0x21 && v <= 0x7E && v != '*' && v != '\'' && v != '%' && v != '"') {
                encoded.append((char) v);
            } else {
                encoded.append(String.format("%%%02X", v));
            }
        }
        return "attachment; filename=\"" + asciiFallback + "\"; filename*=UTF-8''" + encoded;
    }

    /** Content type from trusted upload metadata (detected from bytes at A7.6.2 upload). */
    private static MediaType mediaTypeFor(String fileType) {
        try {
            return MediaType.parseMediaType(fileType);
        } catch (Exception e) {
            // Safe fallback for legacy rows with unusual stored types.
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
