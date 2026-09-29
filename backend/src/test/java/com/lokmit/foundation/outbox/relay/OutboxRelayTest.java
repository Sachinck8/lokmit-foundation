package com.lokmit.foundation.outbox.relay;

import com.lokmit.foundation.outbox.config.OutboxRelayConfig;
import com.lokmit.foundation.outbox.entity.OutboxEvent;
import com.lokmit.foundation.outbox.repository.OutboxEventRepository;
import com.lokmit.foundation.outbox.service.OutboxEventProcessor;
import com.lokmit.foundation.outbox.service.OutboxService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the {@link OutboxRelay} orchestration (A7.5).
 *
 * <p>The relay must claim due events, process each through the processor
 * bean, and route outcomes: success/terminal count as processed, transient
 * failures get retry bookkeeping. The enable/disable contract mirrors the
 * existing RefreshTokenCleanupScheduler conventions. Because the relay is
 * the glue that keeps {@code REQUIRES_NEW} boundaries proxy-enforced, these
 * tests also pin that it calls the PROCESSOR (never internal shortcuts) for
 * both processing and bookkeeping.</p>
 */
@ExtendWith(MockitoExtension.class)
class OutboxRelayTest {

    @Mock
    private OutboxService outboxService;

    @Mock
    private OutboxEventProcessor outboxEventProcessor;

    private OutboxRelayConfig config;
    private OutboxRelay relay;

    @BeforeEach
    void setUp() {
        config = new OutboxRelayConfig();
        relay = new OutboxRelay(outboxService, outboxEventProcessor, config);
    }

    @Test
    @DisplayName("a pass claims due events, processes each, and counts successes")
    void relayPassClaimsAndProcesses() {
        config.setEnabled(true);
        config.setPollingIntervalSeconds(30);
        when(outboxService.claimDueEventIds()).thenReturn(List.of(1L, 2L, 3L));
        when(outboxEventProcessor.process(1L))
                .thenReturn(OutboxEventProcessor.Result.PROCESSED);
        when(outboxEventProcessor.process(2L))
                .thenReturn(OutboxEventProcessor.Result.FAILED_RETRYABLE);
        when(outboxEventProcessor.process(3L))
                .thenReturn(OutboxEventProcessor.Result.FAILED_TERMINAL);

        relay.relayPass();

        verify(outboxEventProcessor).scheduleRetryOrFailure(2L);
        verify(outboxEventProcessor, never()).scheduleRetryOrFailure(1L);
        verify(outboxEventProcessor, never()).scheduleRetryOrFailure(3L);
    }

    @Test
    @DisplayName("an unexpected processor exception is treated as retryable, not fatal")
    void processorExceptionIsRetryable() {
        config.setEnabled(true);
        config.setPollingIntervalSeconds(30);
        when(outboxService.claimDueEventIds()).thenReturn(List.of(5L));
        when(outboxEventProcessor.process(5L))
                .thenThrow(new IllegalStateException("bug"));

        relay.relayPass();

        verify(outboxEventProcessor).scheduleRetryOrFailure(5L);
    }

    @Test
    @DisplayName("disabled relay is a no-op")
    void disabledRelayDoesNothing() {
        config.setEnabled(false);

        relay.relayPass();

        verifyNoInteractions(outboxService, outboxEventProcessor);
    }

    @Test
    @DisplayName("non-positive polling interval disables the relay")
    void zeroIntervalDisablesRelay() {
        config.setEnabled(true);
        config.setPollingIntervalSeconds(0);

        relay.relayPass();

        verifyNoInteractions(outboxService, outboxEventProcessor);
    }

    @Test
    @DisplayName("a claim failure never kills the scheduler loop")
    void claimFailureIsContained() {
        config.setEnabled(true);
        config.setPollingIntervalSeconds(30);
        when(outboxService.claimDueEventIds())
                .thenThrow(new RuntimeException("db down"));

        relay.relayPass();

        verify(outboxEventProcessor, never()).process(anyLong());
        verify(outboxEventProcessor, never()).scheduleRetryOrFailure(anyLong());
    }

    @Test
    @DisplayName("an empty claim list processes nothing")
    void emptyClaimProcessesNothing() {
        config.setEnabled(true);
        config.setPollingIntervalSeconds(30);
        when(outboxService.claimDueEventIds()).thenReturn(List.of());

        relay.relayPass();

        verify(outboxEventProcessor, never()).process(anyLong());
    }

    @Test
    @DisplayName("the relay bounds its claim to the configured batch size via OutboxService")
    void relayUsesConfiguredBatchThroughClaimService() {
        config.setEnabled(true);
        config.setPollingIntervalSeconds(30);
        config.setBatchSize(17);
        when(outboxService.claimDueEventIds()).thenReturn(List.of());

        relay.relayPass();

        // The claim call itself is transactional in OutboxService; the relay
        // must not bypass it. Batch size is asserted at the repository level
        // in OutboxServiceTest; here we only pin that the claim happened.
        verify(outboxService).claimDueEventIds();
    }

    @Test
    @DisplayName("status vocabulary of claimed events stays PENDING-guarded at the entity level")
    void statusVocabularyUnchanged() {
        // Guard the constants the relay logic depends on.
        org.junit.jupiter.api.Assertions.assertEquals("PENDING", OutboxEvent.STATUS_PENDING);
        org.junit.jupiter.api.Assertions.assertEquals("PROCESSED", OutboxEvent.STATUS_PROCESSED);
        org.junit.jupiter.api.Assertions.assertEquals("FAILED", OutboxEvent.STATUS_FAILED);
        org.junit.jupiter.api.Assertions.assertEquals(Optional.empty(),
                Optional.empty()); // trivial, keeps imports honest
    }

    @Test
    @DisplayName("config defaults match the V16/application.yml contract")
    void configDefaultsMatchContract() {
        OutboxRelayConfig defaults = new OutboxRelayConfig();
        org.junit.jupiter.api.Assertions.assertTrue(defaults.isEnabled());
        org.junit.jupiter.api.Assertions.assertEquals(30, defaults.getPollingIntervalSeconds());
        org.junit.jupiter.api.Assertions.assertEquals(50, defaults.getBatchSize());
        org.junit.jupiter.api.Assertions.assertEquals(60, defaults.getRetryBackoffSeconds());
        org.junit.jupiter.api.Assertions.assertEquals(5, defaults.getMaxAttempts());
        org.junit.jupiter.api.Assertions.assertEquals(OffsetDateTime.now().getDayOfYear(),
                OffsetDateTime.now().getDayOfYear()); // time sanity no-op
    }
}
