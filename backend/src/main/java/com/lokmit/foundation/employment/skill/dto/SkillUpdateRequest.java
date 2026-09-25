package com.lokmit.foundation.employment.skill.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Schema(description = "Payload for partially updating a skill")
@Getter
@Setter
public class SkillUpdateRequest {

    @Schema(description = "Skill name")
    @Size(max = 100)
    private String name;

    @Schema(description = "Status")
    @Pattern(regexp = "ACTIVE|INACTIVE",
            message = "status must be ACTIVE or INACTIVE")
    private String status;
}
