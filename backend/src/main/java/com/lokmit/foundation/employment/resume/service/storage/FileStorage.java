package com.lokmit.foundation.employment.resume.service.storage;

import org.springframework.lang.Nullable;

/**
 * Minimal provider-neutral storage abstraction for resume files
 * (A7.6.1 foundation). Business logic (A7.6.2+) depends on this
 * interface, not on how bytes are physically kept — the locked A7.6
 * decision is {@link DatabaseFileStorage} (PostgreSQL BYTEA); a future
 * object-store implementation would be additive without touching
 * callers.
 *
 * <p>Keys are opaque storage identities (e.g.
 * {@code resumes/{resumeId}/{uuid}}), never user-controlled paths and
 * never public URLs. Only the operations the foundation and its known
 * successors need are defined — deliberately no speculative list/copy/
 * presign surface.</p>
 */
public interface FileStorage {

    /**
     * Stores the exact bytes under {@code key}. Implementations must
     * persist atomically with the caller's transaction where the
     * backend allows it (the database implementation does — bytes and
     * metadata commit together, so no orphan bytes can exist).
     *
     * @return the stored key (same value, for fluent metadata assembly)
     */
    String store(String key, byte[] content);

    /**
     * Loads the exact bytes previously stored under {@code key}.
     *
     * @return the bytes, or {@code null} when nothing is stored under
     *         the key (callers map this to their not-found semantics)
     */
    @Nullable
    byte[] load(String key);

    /**
     * Removes the bytes stored under {@code key}. Must be idempotent:
     * deleting an already-absent key is a no-op, not an error (safe
     * compensation semantics for later phases). Deletion POLICY is not
     * decided here — this is only the physical remove primitive.
     */
    void delete(String key);
}
