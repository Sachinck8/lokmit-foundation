package com.lokmit.foundation.employment.resume.repository;

import com.lokmit.foundation.employment.resume.entity.ResumeFileBlob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository for the V17 'resumes_file_blobs' table (A7.6.1 foundation).
 * The ONLY access path to resume bytes — deliberately separated from
 * {@link ResumeRepository} so metadata listing can never load BYTEA
 * content.
 */
public interface ResumeFileBlobRepository extends JpaRepository<ResumeFileBlob, Long> {

    /**
     * Loads the raw bytes for one resume. The V17 FK
     * (ON DELETE CASCADE) guarantees the row cannot exist without its
     * resume, so an empty result means "no stored bytes" (or the resume
     * itself is gone) — callers in later phases map that to 404.
     */
    Optional<ResumeFileBlob> findByResumeId(Long resumeId);
}
