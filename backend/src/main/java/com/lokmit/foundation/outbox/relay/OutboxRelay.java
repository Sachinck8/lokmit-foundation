package com.lokmit.foundation.outbox.relay;

import com.lokmit.foundation.outbox.config.OutboxRelayConfig;
import com.lokmit.foundation.outbox.service.OutboxEventProcessor;
import com.lokmit.foundation.outbox.service.OutboxService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Scheduled outbox relay (A7.5). Follows the
 * {@link com.lokmit.foundation.security.service.RefreshTokenCleanupScheduler}
 * conventions: config-driven interval, no-op when disabled, safe to run on
 * every instance.
 *
 * <p>Multi-instance safety relies on PostgreSQL-level row locking: claiming
 * uses {@code SELECT ... FOR UPDATE SKIP LOCKED} (see
 * {@link com.lokmit.foundation.outbox.repository.OutboxEventRepository}), so
 * concurrent instances claim disjoint subsets of due rows and never process
 * the same event twice.</p>
 *
 * <p>Transaction topology (why this orchestrator exists): the claim runs in
 * one short transaction; each claimed event is then processed in its own
 * {@code REQUIRES_NEW} transaction, and retry bookkeeping in another. All
 * three boundaries are only enforced because they are invoked EXTERNALLY
 * through Spring proxies — a self-invocation (scheduler → same-bean helper)
 * would silently collapse everything into one transaction, where a single
 * poison event would roll back the entire batch and permanently wedge the
 * queue. {@code claim → process → bookkeep} therefore lives here as plain
 * orchestration over three injected beans.</p>
 *
 * <p>Scheduling: the fixed delay is derived from
 * {@code app.outbox.relay.polling-interval-seconds} and clamped to a minimum
 * of 1 second, so a misconfigured sub-second value can never produce a
 * degenerate zero-delay schedule. {@code enabled=false} or
 * {@code polling-interval-seconds <= 0} then disables all work via the body
 * guard in {@link #relayPass()} — the scheduled method still fires on its
 * interval but is a cheap no-op, matching the RefreshTokenCleanupScheduler
 * convention.</p>
 *
 * <p>The relay ONLY materializes in-app notifications. No external
 * delivery (email/SMS/WhatsApp) occurs here, per the locked A7.5
 * architecture.</p>
 */
@Component
public class OutboxRelay {

    private static final Logger LOG = LoggerFactory.getLogger(OutboxRelay.class);

    private final OutboxService outboxService;
    private final OutboxEventProcessor outboxEventProcessor;
    private final OutboxRelayConfig config;

    public OutboxRelay(OutboxService outboxService,
                       OutboxEventProcessor outboxEventProcessor,
                       OutboxRelayConfig config) {
        this.outboxService = outboxService;
        this.outboxEventProcessor = outboxEventProcessor;
        this.config = config;
    }

    /**
     * One relay pass. The polling interval is clamped to >= 1 second (see
     * the class javadoc); disabled when {@code app.outbox.relay.enabled=false}
     * or the polling interval is <= 0, which also keeps tests deterministic
     * when they invoke {@link #relayPass()} directly.
     */
    @Scheduled(fixedDelayString =
            "#{T(java.time.Duration).ofSeconds(Math.max(${app.outbox.relay.polling-interval-seconds:30}, 1)).toMillis()}")
    public void relayPass() {
        if (!config.isEnabled() || config.getPollingIntervalSeconds() <= 0) {
            return;
        }
        try {
            List<Long> claimed = outboxService.claimDueEventIds();
            int succeeded = 0;
            for (Long eventId : claimed) {
                OutboxEventProcessor.Result result;
                try {
                    result = outboxEventProcessor.process(eventId);
                } catch (Exception e) {
                    // The processor never throws in normal operation; this
                    // defensive catch converts a hypothetical relay bug into
                    // a retryable outcome instead of killing the loop.
                    LOG.warn("Outbox event {} processing threw unexpectedly: {}",
                            eventId, e.getMessage());
                    result = OutboxEventProcessor.Result.FAILED_RETRYABLE;
                }
                switch (result) {
                    case PROCESSED, FAILED_TERMINAL -> succeeded++;
                    case FAILED_RETRYABLE ->
                            outboxEventProcessor.scheduleRetryOrFailure(eventId);
                }
            }
            if (succeeded > 0) {
                LOG.info("Outbox relay processed {} event(s)", succeeded);
            }
        } catch (Exception e) {
            // A failed pass must never kill the scheduler; the next pass retries.
            LOG.warn("Outbox relay pass failed: {}", e.getMessage());
        }
    }
}
