package com.lokmit.foundation.employment.jobcategory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Schema(description = "Payload for creating a job category")
@Getter
@Setter
public class JobCategoryCreateRequest {

    @Schema(description = "Category name", example = "Engineering")
    @NotBlank
    @Size(max = 100)
    private String name;

    @Schema(description = "URL slug (immutable after creation)")
    @NotBlank
    @Size(max = 120)
    @Pattern(regexp = "^[a-z0-9]+(-[a-z0-9]+)*$",
            message = "slug must be lowercase alphanumeric words separated by hyphens")
    private String slug;

    @Schema(description = "Description")
    @Size(max = 500)
    private String description;

    @Schema(description = "Display order")
    @NotNull
    private Integer displayOrder;

    @Schema(description = "Status", defaultValue = "ACTIVE")
    @Pattern(regexp = "ACTIVE|INACTIVE",
            message = "status must be ACTIVE or INACTIVE")
    private String status;
}
