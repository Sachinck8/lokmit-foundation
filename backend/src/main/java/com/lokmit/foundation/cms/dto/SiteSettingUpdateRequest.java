package com.lokmit.foundation.cms.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Partial update of a site setting. The immutable identity ({@code settingKey})
 * is deliberately absent so it can never be renamed through this endpoint;
 * {@code valueProvided}/{@code descriptionProvided} distinguish "omitted"
 * (leave unchanged) from "present with JSON null" (clear the description).
 */
@Getter
@Setter
@Schema(description = "Partial site setting update. Omitted fields stay unchanged.")
public class SiteSettingUpdateRequest {

    @Size(max = 100000, message = "Setting value must not exceed 100000 characters")
    @Schema(description = "New setting value")
    private String settingValue;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    @Schema(description = "Description. Set explicitly to null to clear it.")
    private String description;

    @JsonIgnore
    @Schema(hidden = true)
    private boolean valueProvided;

    @JsonIgnore
    @Schema(hidden = true)
    private boolean descriptionProvided;

    public void setSettingValue(String settingValue) {
        this.settingValue = settingValue;
        this.valueProvided = true;
    }

    public void setDescription(String description) {
        this.description = description;
        this.descriptionProvided = true;
    }
}
