package com.lokmit.foundation.employment.resume.dto;

import com.lokmit.foundation.employment.resume.entity.Resume;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.time.OffsetDateTime;

/**
 * Safe resume metadata representation for the candidate self-service API
 * (A7.6.3). Exposes exactly the fields a candidate may see about their own
 * resume — and nothing about where or how the bytes are stored:
 *
 * <ul>
 *   <li>NO {@code storageKey} (internal storage identity)</li>
 *   <li>NO {@code fileUrl} (the legacy V8 column / internal marker)</li>
 *   <li>NO blob, provider or database information</li>
 * </ul>
 */
@Getter
@Schema(description = "Metadata of an uploaded resume (no storage internals)")
public class ResumeResponse {

    @Schema(description = "Resume id", example = "42")
    private final Long id;

    @Schema(description = "Owning candidate id (server-derived, never client-chosen)", example = "7")
    private final Long candidateId;

    @Schema(description = "Original (validated) filename, display only", example = "my-resume.pdf")
    private final String fileName;

    @Schema(description = "Detected content type from the actual bytes", example = "application/pdf")
    private final String fileType;

    @Schema(description = "Size in bytes", example = "123456")
    private final Long fileSizeBytes;

    @Schema(description = "Whether this is the candidate's active resume")
    private final boolean active;

    @Schema(description = "Upload timestamp")
    private final OffsetDateTime createdAt;

    @Schema(description = "SHA-256 checksum of the stored bytes (integrity)", example = "e3b0c4…")
    private final String checksumSha256;

    private ResumeResponse(Builder b) {
        this.id = b.id;
        this.candidateId = b.candidateId;
        this.fileName = b.fileName;
        this.fileType = b.fileType;
        this.fileSizeBytes = b.fileSizeBytes;
        this.active = b.active;
        this.createdAt = b.createdAt;
        this.checksumSha256 = b.checksumSha256;
    }

    public static Builder builder() { return new Builder(); }

    public static ResumeResponse from(Resume r) {
        return builder()
                .id(r.getId())
                .candidateId(r.getCandidateId())
                .fileName(r.getFileName())
                .fileType(r.getFileType())
                .fileSizeBytes(r.getFileSizeBytes())
                .active(r.isActive())
                .createdAt(r.getCreatedAt())
                .checksumSha256(r.getChecksumSha256())
                .build();
    }

    public static class Builder {
        private Long id;
        private Long candidateId;
        private String fileName;
        private String fileType;
        private Long fileSizeBytes;
        private boolean active;
        private OffsetDateTime createdAt;
        private String checksumSha256;

        public Builder id(Long v) { this.id = v; return this; }
        public Builder candidateId(Long v) { this.candidateId = v; return this; }
        public Builder fileName(String v) { this.fileName = v; return this; }
        public Builder fileType(String v) { this.fileType = v; return this; }
        public Builder fileSizeBytes(Long v) { this.fileSizeBytes = v; return this; }
        public Builder active(boolean v) { this.active = v; return this; }
        public Builder createdAt(OffsetDateTime v) { this.createdAt = v; return this; }
        public Builder checksumSha256(String v) { this.checksumSha256 = v; return this; }
        public ResumeResponse build() { return new ResumeResponse(this); }
    }
}
