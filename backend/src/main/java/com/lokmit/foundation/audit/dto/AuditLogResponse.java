package com.lokmit.foundation.audit.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.time.OffsetDateTime;

/**
 * Safe audit-log view for SUPER_ADMIN readers. The actor is exposed only as
 * an opaque database user id (nullable = system action) — no User entity,
 * email, or authentication material is ever serialized. {@code details} is
 * the validated, redacted JSON object persisted with the record.
 */
@Schema(description = "Immutable administrative audit record.")
@Getter
public class AuditLogResponse {

    private final Long id;
    private final Long actorUserId;
    private final String action;
    private final String entityType;
    private final Long entityId;
    private final String details;
    private final OffsetDateTime createdAt;

    private AuditLogResponse(Builder b) {
        this.id = b.id;
        this.actorUserId = b.actorUserId;
        this.action = b.action;
        this.entityType = b.entityType;
        this.entityId = b.entityId;
        this.details = b.details;
        this.createdAt = b.createdAt;
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private Long actorUserId;
        private String action;
        private String entityType;
        private Long entityId;
        private String details;
        private OffsetDateTime createdAt;

        public Builder id(Long v) { this.id = v; return this; }
        public Builder actorUserId(Long v) { this.actorUserId = v; return this; }
        public Builder action(String v) { this.action = v; return this; }
        public Builder entityType(String v) { this.entityType = v; return this; }
        public Builder entityId(Long v) { this.entityId = v; return this; }
        public Builder details(String v) { this.details = v; return this; }
        public Builder createdAt(OffsetDateTime v) { this.createdAt = v; return this; }
        public AuditLogResponse build() {
            return new AuditLogResponse(this);
        }
    }
}
