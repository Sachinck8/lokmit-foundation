package com.lokmit.foundation.employment.job.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Schema(description = "Payload for assigning a skill requirement to a job")
@Getter
@Setter
public class JobSkillRequest {

    @Schema(description = "Skill id to assign")
    @NotNull(message = "skillId is required")
    private Long skillId;
}
