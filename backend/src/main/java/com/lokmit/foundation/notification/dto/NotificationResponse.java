package com.lokmit.foundation.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.time.OffsetDateTime;

/**
 * Safe notification view. Only schema-backed, non-sensitive scheduling/
 * content fields are exposed — no recipient identity resolution internals,
 * no security material.
 */
@Schema(description = "In-app notification for the authenticated user.")
@Getter
public class NotificationResponse {

    private final Long id;
    private final String type;
    private final String title;
    private final String body;
    private final String entityType;
    private final Long entityId;
    private final OffsetDateTime readAt;
    private final OffsetDateTime createdAt;

    private NotificationResponse(Builder b) {
        this.id = b.id;
        this.type = b.type;
        this.title = b.title;
        this.body = b.body;
        this.entityType = b.entityType;
        this.entityId = b.entityId;
        this.readAt = b.readAt;
        this.createdAt = b.createdAt;
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private String type;
        private String title;
        private String body;
        private String entityType;
        private Long entityId;
        private OffsetDateTime readAt;
        private OffsetDateTime createdAt;

        public Builder id(Long v) { this.id = v; return this; }
        public Builder type(String v) { this.type = v; return this; }
        public Builder title(String v) { this.title = v; return this; }
        public Builder body(String v) { this.body = v; return this; }
        public Builder entityType(String v) { this.entityType = v; return this; }
        public Builder entityId(Long v) { this.entityId = v; return this; }
        public Builder readAt(OffsetDateTime v) { this.readAt = v; return this; }
        public Builder createdAt(OffsetDateTime v) { this.createdAt = v; return this; }
        public NotificationResponse build() {
            return new NotificationResponse(this);
        }
    }
}
