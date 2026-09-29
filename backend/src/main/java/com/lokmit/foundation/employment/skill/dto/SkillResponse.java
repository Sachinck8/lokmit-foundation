package com.lokmit.foundation.employment.skill.dto;

import com.lokmit.foundation.employment.skill.entity.Skill;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

/** Safe skill representation. */
@Getter
public class SkillResponse {

    private final Long id;
    private final String name;
    private final String status;
    private final java.time.OffsetDateTime createdAt;
    private final java.time.OffsetDateTime updatedAt;

    private SkillResponse(Builder b) {
        this.id = b.id;
        this.name = b.name;
        this.status = b.status;
        this.createdAt = b.createdAt;
        this.updatedAt = b.updatedAt;
    }

    public static Builder builder() { return new Builder(); }

    public static SkillResponse from(Skill s) {
        return builder()
                .id(s.getId())
                .name(s.getName())
                .status(s.getStatus())
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .build();
    }

    public static class Builder {
        private Long id;
        private String name;
        private String status;
        private java.time.OffsetDateTime createdAt;
        private java.time.OffsetDateTime updatedAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder name(String v) { this.name = v; return this; }
        public Builder status(String v) { this.status = v; return this; }
        public Builder createdAt(java.time.OffsetDateTime v) { this.createdAt = v; return this; }
        public Builder updatedAt(java.time.OffsetDateTime v) { this.updatedAt = v; return this; }
        public SkillResponse build() { return new SkillResponse(this); }
    }
}
