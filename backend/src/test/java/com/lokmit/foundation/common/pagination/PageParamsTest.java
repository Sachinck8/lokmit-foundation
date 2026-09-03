package com.lokmit.foundation.common.pagination;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PageParamsTest {

    @Test
    void defaults_shouldBePageZeroSizeTwenty() {
        PageParams params = new PageParams();

        assertThat(params.getPage()).isZero();
        assertThat(params.getSize()).isEqualTo(PageParams.DEFAULT_SIZE);

        Pageable pageable = params.toPageable();
        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(PageParams.DEFAULT_SIZE);
    }

    @Test
    void setters_shouldBeReflectedInPageable() {
        PageParams params = new PageParams();
        params.setPage(3);
        params.setSize(50);

        Pageable pageable = params.toPageable();

        assertThat(pageable.getPageNumber()).isEqualTo(3);
        assertThat(pageable.getPageSize()).isEqualTo(50);
    }

    @Test
    void negativePage_shouldFailValidation() {
        PageParams params = new PageParams();
        params.setPage(-1);

        assertThat(violations(params))
                .extracting(v -> v.getPropertyPath().toString())
                .contains("page");
    }

    @Test
    void zeroSize_shouldFailValidation() {
        PageParams params = new PageParams();
        params.setSize(0);

        assertThat(violations(params))
                .extracting(v -> v.getPropertyPath().toString())
                .contains("size");
    }

    @Test
    void sizeAboveMax_shouldFailValidation() {
        PageParams params = new PageParams();
        params.setSize(PageParams.MAX_SIZE + 1);

        assertThat(violations(params))
                .extracting(v -> v.getPropertyPath().toString())
                .contains("size");
    }

    @Test
    void validParams_shouldPassValidation() {
        PageParams params = new PageParams();
        params.setPage(2);
        params.setSize(PageParams.MAX_SIZE);

        assertThat(violations(params)).isEmpty();
    }

    private List<ConstraintViolation<PageParams>> violations(PageParams params) {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();
            return validator.validate(params).stream().toList();
        }
    }
}