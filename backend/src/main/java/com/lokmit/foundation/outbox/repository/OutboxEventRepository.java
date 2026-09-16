package com.lokmit.foundation.outbox.repository;

import com.lokmit.foundation.outbox.entity.OutboxEvent;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for the V16 'outbox_events' table.
 *
 * <p>Multi-instance safety is built from two pessimistic queries:</p>
 *
 * <ul>
 *   <li>{@link #claimDuePendingEventIds} — the <em>claim</em> query. Rows are
 *       locked with {@code FOR UPDATE SKIP LOCKED} inside a short transaction
 *       that returns only their IDs and commits immediately, releasing the
 *       locks. Concurrent relay instances each lock a disjoint subset of due
 *       PENDING rows — no row is claimed twice and no instance blocks
 *       another. Backed by idx_outbox_events_status_available.</li>
 *   <li>{@link #acquireForProcessing} — the <em>processing</em> re-read. Each
 *       claimed ID is re-fetched with a blocking {@code FOR UPDATE} inside
 *       that event's own short transaction. Under READ_COMMITTED the SELECT
 *       waits for any in-flight writer to commit, then reads the
 *       <em>latest committed</em> version, so two relay passes can never
 *       process the same event concurrently and no update is lost.</li>
 * </ul>
 *
 * <p>{@link #scheduleRetryIfStillPending} increments {@code attempts} and
 * shifts {@code available_at} forward only if the row is still PENDING — the
 * guard makes retry bookkeeping race-free against an event that has already
 * been marked PROCESSED/FAILED by another pass.</p>
 */
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    /**
     * Claims due PENDING rows: locks them with {@code FOR UPDATE SKIP LOCKED}
     * and returns their IDs. The calling transaction must commit immediately
     * after (releasing the row locks) and process the returned IDs one by one
     * in their own transactions. The Pageable bounds the claim to the
     * configured batch size (limit only — no offset, oldest IDs first).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT e.id FROM OutboxEvent e
            WHERE e.status = 'PENDING' AND e.availableAt <= :now
            ORDER BY e.id
            """)
    List<Long> claimDuePendingEventIds(@Param("now") OffsetDateTime now,
                                       Pageable limit);

    /**
     * Blocking re-read used at the start of a per-event processing
     * transaction: waits for any competing writer, then returns the latest
     * committed row with a row lock held for the remainder of the
     * transaction. Re-fetching through the lock also invalidates the stale
     * entity state carried from the claim pass.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM OutboxEvent e WHERE e.id = :id")
    Optional<OutboxEvent> acquireForProcessing(@Param("id") Long id);

    /**
     * Retry gate: increments attempts and pushes {@code available_at} forward
     * only if the event is STILL PENDING (a competing pass may already have
     * marked it PROCESSED or FAILED). Returns 1 when the retry was scheduled,
     * 0 otherwise.
     */
    @Modifying
    @Query("""
            UPDATE OutboxEvent e
            SET e.attempts = e.attempts + 1, e.availableAt = :nextAvailableAt
            WHERE e.id = :id AND e.status = 'PENDING'
            """)
    int scheduleRetryIfStillPending(@Param("id") Long id,
                                    @Param("nextAvailableAt") OffsetDateTime nextAvailableAt);

    /**
     * Terminal-failure gate: marks the event FAILED only if it is still
     * PENDING. Returns 1 when the event was marked, 0 otherwise.
     */
    @Modifying
    @Query("""
            UPDATE OutboxEvent e
            SET e.status = 'FAILED', e.processedAt = :now
            WHERE e.id = :id AND e.status = 'PENDING'
            """)
    int markFailedIfStillPending(@Param("id") Long id, @Param("now") OffsetDateTime now);
}
