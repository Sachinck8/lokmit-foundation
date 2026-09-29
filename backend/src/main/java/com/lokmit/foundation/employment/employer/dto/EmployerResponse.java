package com.lokmit.foundation.employment.employer.dto;

import com.lokmit.foundation.employment.employer.entity.Employer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

/**
 * Safe employer profile representation. Exposes the linked user only as an
 * opaque id — no email, password hash, lockout, token or other identity
 * internals leak through this DTO.
 */
@Getter
public class EmployerResponse {

    private final Long id;
    private final Long userId;
    private final String companyName;
    private final String about;
    private final String websiteUrl;
    private final String logoUrl;
    private final String contactPersonName;
    private final String contactPhone;
    private final String address;
    private final String verificationStatus;
    private final String status;
    private final java.time.OffsetDateTime createdAt;
    private final java.time.OffsetDateTime updatedAt;

    private EmployerResponse(Builder b) {
        this.id = b.id;
        this.userId = b.userId;
        this.companyName = b.companyName;
        this.about = b.about;
        this.websiteUrl = b.websiteUrl;
        this.logoUrl = b.logoUrl;
        this.contactPersonName = b.contactPersonName;
        this.contactPhone = b.contactPhone;
        this.address = b.address;
        this.verificationStatus = b.verificationStatus;
        this.status = b.status;
        this.createdAt = b.createdAt;
        this.updatedAt = b.updatedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static EmployerResponse from(Employer e) {
        return builder()
                .id(e.getId())
                .userId(e.getUser().getId())
                .companyName(e.getCompanyName())
                .about(e.getAbout())
                .websiteUrl(e.getWebsiteUrl())
                .logoUrl(e.getLogoUrl())
                .contactPersonName(e.getContactPersonName())
                .contactPhone(e.getContactPhone())
                .address(e.getAddress())
                .verificationStatus(e.getVerificationStatus())
                .status(e.getStatus())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }

    public static class Builder {
        private Long id;
        private Long userId;
        private String companyName;
        private String about;
        private String websiteUrl;
        private String logoUrl;
        private String contactPersonName;
        private String contactPhone;
        private String address;
        private String verificationStatus;
        private String status;
        private java.time.OffsetDateTime createdAt;
        private java.time.OffsetDateTime updatedAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder userId(Long userId) { this.userId = userId; return this; }
        public Builder companyName(String v) { this.companyName = v; return this; }
        public Builder about(String v) { this.about = v; return this; }
        public Builder websiteUrl(String v) { this.websiteUrl = v; return this; }
        public Builder logoUrl(String v) { this.logoUrl = v; return this; }
        public Builder contactPersonName(String v) { this.contactPersonName = v; return this; }
        public Builder contactPhone(String v) { this.contactPhone = v; return this; }
        public Builder address(String v) { this.address = v; return this; }
        public Builder verificationStatus(String v) { this.verificationStatus = v; return this; }
        public Builder status(String v) { this.status = v; return this; }
        public Builder createdAt(java.time.OffsetDateTime v) { this.createdAt = v; return this; }
        public Builder updatedAt(java.time.OffsetDateTime v) { this.updatedAt = v; return this; }
        public EmployerResponse build() { return new EmployerResponse(this); }
    }
}
