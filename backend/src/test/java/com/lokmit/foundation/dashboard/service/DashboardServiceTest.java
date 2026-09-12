package com.lokmit.foundation.dashboard.service;

import com.lokmit.foundation.dashboard.repository.DashboardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the dashboard service's limit handling (A2, section 13):
 * the requested limit must be clamped into [1, 10] with default 5, so no
 * client-supplied value can widen the recent-activity queries.
 */
class DashboardServiceTest {

    private DashboardRepository repository;
    private DashboardService service;

    @BeforeEach
    void setUp() {
        repository = mock(DashboardRepository.class);
        service = new DashboardService(repository);
    }

    @ParameterizedTest(name = "limit {0} is clamped to {1}")
    @CsvSource({
            "1, 1",
            "3, 3",
            "5, 5",
            "10, 10",
            ", 5",           // absent parameter → default
            "0, 5",          // zero → default
            "-7, 5",         // negative → default
            "11, 10",        // above max → max
            "100000, 10"     // abuse attempt → max
    })
    @DisplayName("Recent-list limits are clamped to [1, 10] with default 5")
    void limitsAreClamped(Integer requested, int expected) {
        when(repository.recentEnquiries(anyInt())).thenReturn(List.of());

        service.getRecentEnquiries(requested);

        ArgumentCaptor<Integer> captor = ArgumentCaptor.forClass(Integer.class);
        verify(repository).recentEnquiries(captor.capture());
        assertThat(captor.getValue()).isEqualTo(expected);
    }

    @Test
    @DisplayName("Recent users and applications pass through the same clamping")
    void clampingAppliesToAllRecentLists() {
        when(repository.recentUsers(anyInt())).thenReturn(List.of());
        when(repository.recentApplications(anyInt())).thenReturn(List.of());

        service.getRecentUsers(100000);
        service.getRecentApplications(null);

        ArgumentCaptor<Integer> users = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> applications = ArgumentCaptor.forClass(Integer.class);
        verify(repository).recentUsers(users.capture());
        verify(repository).recentApplications(applications.capture());
        assertThat(users.getValue()).isEqualTo(10);
        assertThat(applications.getValue()).isEqualTo(5);
    }

    @Test
    @DisplayName("Summary delegates to the repository aggregation")
    void summaryDelegates() {
        when(repository.summaryCounts()).thenReturn(
                com.lokmit.foundation.dashboard.dto.DashboardSummaryResponse.builder().build());

        assertThat(service.getSummary()).isNotNull();
        verify(repository).summaryCounts();
    }
}
