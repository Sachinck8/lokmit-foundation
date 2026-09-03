package com.lokmit.foundation.common.api;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PageResponseTest {

    @Test
    void ofPage_shouldMapContentAndMetadata() {
        Page<String> page = new PageImpl<>(List.of("a", "b"), PageRequest.of(2, 5), 17);

        PageResponse<String> response = PageResponse.of(page);

        assertThat(response.getItems()).containsExactly("a", "b");
        assertThat(response.getPage()).isEqualTo(2);
        assertThat(response.getSize()).isEqualTo(5);
        assertThat(response.getTotalItems()).isEqualTo(17);
        assertThat(response.getTotalPages()).isEqualTo(4); // ceil(17 / 5)
    }

    @Test
    void ofList_shouldComputeTotalPagesWithCeiling() {
        PageResponse<String> response = PageResponse.of(List.of("x"), 3, 10, 25);

        assertThat(response.getItems()).containsExactly("x");
        assertThat(response.getPage()).isEqualTo(3);
        assertThat(response.getSize()).isEqualTo(10);
        assertThat(response.getTotalItems()).isEqualTo(25);
        assertThat(response.getTotalPages()).isEqualTo(3); // ceil(25 / 10)
    }

    @Test
    void ofList_emptyData_shouldReturnZeroTotalPages() {
        PageResponse<String> response = PageResponse.of(List.of(), 0, 20, 0);

        assertThat(response.getItems()).isEmpty();
        assertThat(response.getTotalPages()).isZero();
    }

    @Test
    void ofPage_shouldHandleExactMultiple() {
        Page<String> page = new PageImpl<>(List.of("a", "b"), PageRequest.of(1, 2), 4);

        PageResponse<String> response = PageResponse.of(page);

        assertThat(response.getTotalPages()).isEqualTo(2);
        assertThat(response.getPage()).isEqualTo(1);
    }
}