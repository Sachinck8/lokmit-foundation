-- =====================================================================
-- V17: Resume & file storage foundation (A7.6.1).
--
-- Database-blob storage decision: PostgreSQL BYTEA is the single source
-- of truth for resume file bytes — no filesystem paths, no object-store
-- SDKs, no external providers. Bytes live in a 1:1 companion table so
-- ordinary resume metadata listing never drags BYTEA payloads out of the
-- database, and the blob's lifetime is structurally tied to its row.
--
--   candidates --(fk_resumes_candidate, CASCADE, V8)
--       -> resumes --(fk_resumes_file_blobs_resume, CASCADE, V17)
--             -> resumes_file_blobs
--
-- Deleting a candidate therefore removes its resumes and their bytes
-- atomically; no orphan-prone relationship is introduced.
--
-- resumes gains two identity/integrity columns only (checksum capture
-- and a provider-neutral storage key). checksum_sha256 is populated by
-- A7.6.2's upload validation — no fake value is written here.
--
-- file_url (V8, NOT NULL) stays untouched: it is LEGACY metadata kept
-- only to satisfy the existing schema. DB-backed storage never uses it
-- as an access mechanism — storage identity is storage_key, and bytes
-- are served through authorized backend streaming (later phases).
-- =====================================================================

CREATE TABLE resumes_file_blobs (
    resume_id BIGINT PRIMARY KEY,
    content   BYTEA  NOT NULL,
    CONSTRAINT fk_resumes_file_blobs_resume FOREIGN KEY (resume_id)
        REFERENCES resumes (id) ON DELETE CASCADE,
    CONSTRAINT chk_resumes_file_blobs_size CHECK (
        octet_length(content) > 0 AND octet_length(content) <= 5242880)
);

ALTER TABLE resumes ADD COLUMN checksum_sha256 VARCHAR(64) NULL;
ALTER TABLE resumes ADD COLUMN storage_key     VARCHAR(500) NULL;

-- ---------------------------------------------------------------------
-- No permission seeding in this phase: A7.6.1 creates no endpoints.
-- app.storage.* configuration lives in application.yml / application-prod.yml
-- (default 'db'); candidate-facing authorization is introduced in A7.6.3.
-- ---------------------------------------------------------------------
