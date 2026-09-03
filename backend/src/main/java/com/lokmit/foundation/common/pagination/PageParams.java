package com.lokmit.foundation.common.pagination;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 * Query-parameter binding for page/size on collection endpoints.
 *
 * <p>Defaults: page 0 (zero-based), size 20, capped at {@link #MAX_SIZE}.
 * Controllers bind it as {@code @Valid @ModelAttribute PageParams}.</p>
 */
@Getter
@Setter
public class PageParams {

    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    @Min(value = 0, message = "Page number must be zero or greater")
    private int page = 0;

    @Min(value = 1, message = "Page size must be at least 1")
    @Max(value = MAX_SIZE, message = "Page size must not exceed " + MAX_SIZE)
    private int size = DEFAULT_SIZE;

    /**
     * Converts this parameter object into the Spring Data {@link Pageable}
     * consumed by repositories.
     */
    public Pageable toPageable() {
        return PageRequest.of(page, size);
    }
}