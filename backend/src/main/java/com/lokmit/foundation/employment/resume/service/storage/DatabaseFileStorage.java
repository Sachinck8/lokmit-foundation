package com.lokmit.foundation.employment.resume.service.storage;

import com.lokmit.foundation.employment.resume.entity.ResumeFileBlob;
import com.lokmit.foundation.employment.resume.repository.ResumeFileBlobRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * PostgreSQL BYTEA implementation of {@link FileStorage} — the locked
 * A7.6 storage decision. Bytes are rows in 'resumes_file_blobs' (V17),
 * written in the CALLER's transaction (REQUIRED): metadata and bytes
 * commit or roll back together, so the classic orphan-file failure modes
 * (file written but row rolled back, row committed but file lost) cannot
 * occur by construction.
 *
 * <p>Storage keys are opaque identities — the database locates bytes by
 * the {@code resume_id} primary key, so {@code load}/{@code delete} map
 * the key back to that id. A future object-store implementation would
 * replace this bean; callers do not change.</p>
 */
@Service
public class DatabaseFileStorage implements FileStorage {

    private final ResumeFileBlobRepository blobRepository;

    public DatabaseFileStorage(ResumeFileBlobRepository blobRepository) {
        this.blobRepository = blobRepository;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public String store(String key, byte[] content) {
        Long resumeId = resumeIdFrom(key);
        ResumeFileBlob blob = new ResumeFileBlob();
        blob.setResumeId(resumeId);
        blob.setContent(content);
        blobRepository.save(blob);
        return key;
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    public byte[] load(String key) {
        Optional<ResumeFileBlob> blob = blobRepository.findByResumeId(resumeIdFrom(key));
        return blob.map(ResumeFileBlob::getContent).orElse(null);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void delete(String key) {
        // Idempotent: the V17 FK cascade already removed the row when the
        // resume was deleted; deleting an absent key is a no-op.
        blobRepository.deleteById(resumeIdFrom(key));
    }

    /**
     * Resolves the resume id from the storage key
     * ({@code resumes/{resumeId}/{opaque}}). Throws for foreign-shaped
     * keys: the database implementation can only address rows by id, and
     * silently ignoring a malformed key would hide caller bugs.
     */
    private Long resumeIdFrom(String key) {
        String[] segments = key.split("/");
        if (segments.length != 3 || !"resumes".equals(segments[0])) {
            throw new IllegalArgumentException(
                    "Unsupported storage key for database storage: " + key);
        }
        try {
            return Long.parseLong(segments[1]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Unsupported storage key for database storage: " + key);
        }
    }
}
