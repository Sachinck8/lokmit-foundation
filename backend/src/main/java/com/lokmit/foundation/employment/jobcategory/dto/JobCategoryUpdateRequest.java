package com.lokmit.foundation.employment.jobcategory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Job category partial update. The slug is deliberately absent — it is the
 * URL identity and immutable after creation, consistent with A5/A6
 * category conventions.
 */
@Schema(description = "Payload for partially updating a job category")
@Getter
@Setter
public class JobCategoryUpdateRequest {

    @Schema(description = "Category name")
    @Size(max = 100)
    private String name;

    @Schema(description = "Description")
    @Size(max = 500)
    private String description;

    @Schema(description = "Display order")
    private Integer displayOrder;

    @Schema(description = "Status")
    @Pattern(regexp = "ACTIVE|INACTIVE",
            message = "status must be ACTIVE or INACTIVE")
    private String status;
}
