package com.lokmit.foundation.employment.resume.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration for secure resume upload validation (A7.6.2), bound from
 * the {@code app.upload.*} property namespace. Mirrors the A7.6.1
 * {@code StorageProperties} convention: a single @ConfigurationProperties
 * bean with safe defaults.
 *
 * <p>Scope is deliberately small: the 5 MiB application limit and the
 * servlet multipart limits that back it. Allowed formats and byte-level
 * format rules are validation CODE, not tunable policy (they live in
 * {@code ResumeContentValidator}); rate limiting and authorization are
 * A7.6.3+ concerns.</p>
 */
@Component
@ConfigurationProperties(prefix = "app.upload")
public class UploadProperties {

    /**
     * Maximum accepted resume size in bytes (application limit). The
     * servlet multipart limits should be configured to allow a modest
     * envelope above this so the SERVICE-level limit — not the container —
     * produces the controlled 413 error. Default: 5 MiB (5 * 1024 * 1024).
     */
    private long maxFileSizeBytes = 5L * 1024 * 1024;

    public long getMaxFileSizeBytes() {
        return maxFileSizeBytes;
    }

    public void setMaxFileSizeBytes(long maxFileSizeBytes) {
        this.maxFileSizeBytes = maxFileSizeBytes;
    }
}
