package com.lokmit.foundation.employment.candidateskill.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Schema(description = "Payload for assigning a skill to a candidate")
@Getter
@Setter
public class CandidateSkillRequest {

    @Schema(description = "Skill id to assign")
    @NotNull(message = "skillId is required")
    private Long skillId;

    @Schema(description = "Optional proficiency level")
    @Pattern(regexp = "BEGINNER|INTERMEDIATE|ADVANCED|EXPERT",
            message = "proficiency must be BEGINNER, INTERMEDIATE, ADVANCED or EXPERT")
    private String proficiency;
}
