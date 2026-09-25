package com.lokmit.foundation.cms.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;

/** Safe admin view of a site setting row. */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Site setting as visible to authorized administrators")
public class SiteSettingResponse {

    @Schema(description = "Setting identifier", example = "1")
    private Long id;

    @Schema(description = "Unique setting key", example = "site.contact_email")
    private String settingKey;

    @Schema(description = "Current setting value")
    private String settingValue;

    @Schema(description = "Optional human-readable description of the setting")
    private String description;

    @Schema(description = "When the setting was created")
    private OffsetDateTime createdAt;

    @Schema(description = "When the setting was last updated")
    private OffsetDateTime updatedAt;
}
