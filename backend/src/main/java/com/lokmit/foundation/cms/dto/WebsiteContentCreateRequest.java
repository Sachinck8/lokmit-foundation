package com.lokmit.foundation.cms.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Creation payload for a website content section. The composite unique key
 * (page_key, section_key) is enforced by the database and pre-checked in the
 * service for a clean 409. Status always starts as DRAFT; publishing happens
 * through the dedicated lifecycle transition endpoint.
 */
@Getter
@Setter
@Schema(description = "Create a website content section. New sections always start as DRAFT.")
public class WebsiteContentCreateRequest {

    @NotBlank(message = "Page key is required")
    @Size(max = 100, message = "Page key must not exceed 100 characters")
    @Pattern(regexp = "[a-z0-9_.-]+", message = "Page key may contain lowercase letters, digits, dot, dash, underscore")
    @Schema(description = "Page identifier key", example = "home")
    private String pageKey;

    @NotBlank(message = "Section key is required")
    @Size(max = 100, message = "Section key must not exceed 100 characters")
    @Pattern(regexp = "[a-z0-9_.-]+", message = "Section key may contain lowercase letters, digits, dot, dash, underscore")
    @Schema(description = "Section identifier key", example = "hero")
    private String sectionKey;

    @Size(max = 255, message = "Title must not exceed 255 characters")
    @Schema(description = "Section title")
    private String title;

    @Size(max = 100000, message = "Content JSON must not exceed 100000 characters")
    @Schema(description = "Section body as a JSON object/string, e.g. {\"headline\":\"Welcome\"}")
    private String contentJson;
}
