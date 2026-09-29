package com.lokmit.foundation.outbox.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration for the outbox relay (A7.5), bound from the
 * {@code app.outbox.relay.*} property namespace. Mirrors the
 * RefreshTokenCleanupConfig convention: {@code pollingIntervalSeconds <= 0}
 * disables the relay entirely (no scheduling, no work), keeping tests and
 * deployments deterministic.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.outbox.relay")
public class OutboxRelayConfig {

    /** Relay on/off switch; disabled when the polling interval is <= 0. */
    private boolean enabled = true;

    /** Seconds between relay passes; <= 0 disables the relay. */
    private int pollingIntervalSeconds = 30;

    /** Maximum rows claimed per pass. */
    private int batchSize = 50;

    /** Base backoff (seconds) after a failed processing attempt. */
    private int retryBackoffSeconds = 60;

    /** Attempts at which an event is marked FAILED and never retried. */
    private int maxAttempts = 5;
}
