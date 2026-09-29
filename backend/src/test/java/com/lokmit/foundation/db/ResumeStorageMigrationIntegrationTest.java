package com.lokmit.foundation.db;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * A7.6.1 integration test for the V17 resume-storage migration, executed
 * against a throwaway schema in the configured development database — the
 * same real-PostgreSQL approach as {@link FlywayMigrationIntegrationTest}
 * (skipped when PostgreSQL is not reachable; no fake database behavior).
 *
 * <p>Verifies: the V17 table exists, {@code resume_id} is the primary key,
 * the FK to resumes is ON DELETE CASCADE, the BYTEA size CHECK enforces
 * both bounds, the two new resumes columns exist without touching legacy
 * V8 columns, the full Candidate → Resume → Blob cascade chain, and that
 * resume metadata remains queryable without any blob row or access.</p>
 */
class ResumeStorageMigrationIntegrationTest {

    private static final String IT_SCHEMA = "lokmit_it_v17";

    private static final String URL = resolve("DB_URL",
            "jdbc:postgresql://localhost:5432/lokmit_foundation");
    private static final String USER = resolve("DB_USERNAME", "lokmit_app");
    private static final String PASSWORD = resolve("DB_PASSWORD", "");

    private Connection connection;
    private long candidateId;

    /**
     * ONE deterministic @BeforeEach: reachability assumption FIRST (so an
     * unreachable PostgreSQL skips cleanly, exactly like
     * {@link FlywayMigrationIntegrationTest}), then a fresh per-test schema
     * migration. JUnit 5 does not guarantee order across multiple
     * @BeforeEach methods, so they must not be split.
     */
    @BeforeEach
    void migrateFreshSchema() throws SQLException {
        boolean reachable;
        try (Connection ignored = DriverManager.getConnection(URL, USER, PASSWORD)) {
            reachable = true;
        } catch (SQLException e) {
            reachable = false;
        }
        assumeTrue(reachable,
                "PostgreSQL not reachable or credentials not configured — V17 integration tests skipped");

        connection = DriverManager.getConnection(URL, USER, PASSWORD);
        dropSchema();
        try (Statement st = connection.createStatement()) {
            st.execute("CREATE SCHEMA " + IT_SCHEMA);
        }
        Flyway.configure()
                .dataSource(URL, USER, PASSWORD)
                .schemas(IT_SCHEMA)
                .createSchemas(true)
                .locations("filesystem:src/main/resources/db/migration")
                .load()
                .migrate();
        try (Statement st = connection.createStatement()) {
            st.execute("SET search_path TO " + IT_SCHEMA);
        }
    }

    @AfterEach
    void tearDown() throws SQLException {
        if (connection == null) {
            return; // assumption skipped before the connection existed
        }
        try {
            dropSchema();
        } finally {
            connection.close();
        }
    }

    private void dropSchema() throws SQLException {
        try (Statement st = connection.createStatement()) {
            st.execute("DROP SCHEMA IF EXISTS " + IT_SCHEMA + " CASCADE");
        } catch (SQLException e) {
            // best effort — the Flyway test convention drops its schema the same way
            System.err.println("Warning: could not drop " + IT_SCHEMA + ": " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // seed helpers
    // ------------------------------------------------------------------

    private void seedCandidate() throws SQLException {
        // CANDIDATE role already exists in V2 — insert a user + profile only.
        try (Statement st = connection.createStatement()) {
            st.execute("""
                    INSERT INTO users (email, password_hash, full_name, user_type, status, email_verified)
                    VALUES ('v17-candidate@example.org',
                            '$2a$10$storedhashnotusedbyjwtfilter012345678901234567890123',
                            'V17 Candidate', 'CANDIDATE', 'ACTIVE', true)
                    """);
            st.execute("""
                    INSERT INTO candidates (user_id)
                    SELECT id FROM users WHERE email = 'v17-candidate@example.org'
                    """);
        }
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT id FROM candidates WHERE user_id = "
                        + "(SELECT id FROM users WHERE email = 'v17-candidate@example.org')");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            candidateId = rs.getLong(1);
        }
    }

    private long insertActiveResume() throws SQLException {
        return insertResume(true);
    }

    private long insertResume(boolean active) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO resumes (candidate_id, file_url, file_name, is_active, created_at) "
                        + "VALUES (?, 'legacy/v17-seed.pdf', 'seed.pdf', ?, now()) RETURNING id")) {
            ps.setLong(1, candidateId);
            ps.setBoolean(2, active);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    private void insertBlob(long resumeId, int sizeBytes) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO resumes_file_blobs (resume_id, content) VALUES (?, repeat('A', ?)::bytea)")) {
            ps.setLong(1, resumeId);
            ps.setInt(2, sizeBytes);
            ps.executeUpdate();
        }
    }

    // ------------------------------------------------------------------
    // tests
    // ------------------------------------------------------------------

    @Test
    @DisplayName("V17 applies cleanly and creates resumes_file_blobs")
    void migrationAppliesAndCreatesTable() throws SQLException {
        assertTrue(tableExists("resumes_file_blobs"), "resumes_file_blobs must exist after V17");
    }

    @Test
    @DisplayName("resume_id is the primary key and the FK to resumes is ON DELETE CASCADE")
    void primaryKeyAndCascadeFk() throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT contype, confdeltype FROM pg_constraint "
                        + "WHERE conrelid = ?::regclass AND conname = 'resumes_file_blobs_pkey'")) {
            ps.setString(1, IT_SCHEMA + ".resumes_file_blobs");
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next(), "resumes_file_blobs_pkey (resume_id PK) must exist");
                assertEquals("p", rs.getString(1), "must be a primary key constraint");
            }
        }
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT confdeltype FROM pg_constraint "
                        + "WHERE conrelid = ?::regclass AND conname = 'fk_resumes_file_blobs_resume'")) {
            ps.setString(1, IT_SCHEMA + ".resumes_file_blobs");
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next(), "fk_resumes_file_blobs_resume must exist");
                assertEquals("c", rs.getString(1), "FK must be ON DELETE CASCADE");
            }
        }
    }

    @Test
    @DisplayName("BYTEA content must be non-empty and at most 5 MB (both CHECK directions)")
    void blobSizeCheck() throws SQLException {
        seedCandidate();
        long resumeId = insertActiveResume();

        // empty content → rejected by chk_resumes_file_blobs_size
        assertThrows(SQLException.class, () -> insertBlob(resumeId, 0),
                "empty content must violate chk_resumes_file_blobs_size");
        // boundary 1 byte → accepted
        insertBlob(resumeId, 1);
        // boundary 5 MB exactly on a second resume → accepted
        long second = insertResume(false);
        insertBlob(second, 5242880);
        // 5 MB + 1 byte → rejected
        long third = insertResume(false);
        assertThrows(SQLException.class, () -> insertBlob(third, 5242881),
                "content above 5 MB must violate chk_resumes_file_blobs_size");
    }

    @Test
    @DisplayName("V17 adds checksum_sha256 and storage_key to resumes and leaves V8 columns untouched")
    void newResumeColumnsExist() throws SQLException {
        assertTrue(columnExists("resumes", "checksum_sha256"), "checksum_sha256 must exist");
        assertTrue(columnExists("resumes", "storage_key"), "storage_key must exist");
        assertFalse(columnExists("resumes_file_blobs", "storage_provider"),
                "storage_provider must NOT exist");
        assertFalse(columnExists("resumes_file_blobs", "updated_at"),
                "updated_at must NOT exist");

        // Legacy NOT NULL of file_url survives V17 untouched.
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT is_nullable FROM information_schema.columns "
                        + "WHERE table_schema = ? AND table_name = 'resumes' AND column_name = 'file_url'")) {
            ps.setString(1, IT_SCHEMA);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                assertEquals("NO", rs.getString(1), "file_url must remain NOT NULL (V8 untouched)");
            }
        }
    }

    @Test
    @DisplayName("Candidate → Resume → Blob cascade chain removes bytes end to end")
    void cascadeChains() throws SQLException {
        seedCandidate();
        long kept = insertActiveResume();
        long removed = insertResume(false); // inactive: unique index unaffected
        insertBlob(kept, 10);
        insertBlob(removed, 10);

        // Deleting one resume removes exactly its blob (FK CASCADE).
        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM resumes WHERE id = ?")) {
            ps.setLong(1, removed);
            ps.executeUpdate();
        }
        assertEquals(0, blobCount(removed), "blob must cascade with its resume");
        assertEquals(1, blobCount(kept), "unrelated blob must survive");

        // Deleting the candidate removes the remaining resume AND its blob.
        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM candidates WHERE id = ?")) {
            ps.setLong(1, candidateId);
            ps.executeUpdate();
        }
        assertEquals(0, blobCount(kept), "blob must cascade through candidate deletion");
        assertEquals(0, scalar("SELECT count(*) FROM resumes WHERE candidate_id = ?",
                candidateId), "resumes must cascade with the candidate (V8)");
    }

    @Test
    @DisplayName("resume metadata is queryable without any blob row or blob access")
    void metadataDoesNotRequireBlob() throws SQLException {
        seedCandidate();
        long resumeId = insertActiveResume();

        // No blob exists — a metadata-only projection works and returns NULL storage_key.
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT file_name, is_active, storage_key FROM resumes WHERE id = ?")) {
            ps.setLong(1, resumeId);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next(), "metadata row must exist without blob");
                assertTrue(rs.getBoolean("is_active"));
                assertNull(rs.getObject("storage_key"), "storage_key starts NULL");
            }
        }
        assertEquals(0, blobCount(resumeId), "no blob row must exist");

        // With a blob present, metadata-only projection still never selects the BYTEA.
        insertBlob(resumeId, 2048);
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT file_name, file_size_bytes, checksum_sha256 FROM resumes WHERE id = ?")) {
            ps.setLong(1, resumeId);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                assertNull(rs.getObject("checksum_sha256"), "checksum stays NULL until A7.6.2");
            }
        }
        assertEquals(2048, blobSize(resumeId), "blob bytes round-trip at the DB level");
    }

    // ------------------------------------------------------------------
    // inspection helpers
    // ------------------------------------------------------------------

    private boolean tableExists(String table) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT 1 FROM information_schema.tables WHERE table_schema = ? AND table_name = ?")) {
            ps.setString(1, IT_SCHEMA);
            ps.setString(2, table);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private boolean columnExists(String table, String column) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT 1 FROM information_schema.columns "
                        + "WHERE table_schema = ? AND table_name = ? AND column_name = ?")) {
            ps.setString(1, IT_SCHEMA);
            ps.setString(2, table);
            ps.setString(3, column);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private int blobCount(long resumeId) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT count(*) FROM resumes_file_blobs WHERE resume_id = ?")) {
            ps.setLong(1, resumeId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private int blobSize(long resumeId) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT octet_length(content) FROM resumes_file_blobs WHERE resume_id = ?")) {
            ps.setLong(1, resumeId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private int scalar(String sql, long param) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private static String resolve(String name, String defaultValue) {
        String value = System.getProperty(name);
        if (value != null && !value.isBlank()) {
            return value;
        }
        value = System.getenv(name);
        if (value != null && !value.isBlank()) {
            return value;
        }
        return defaultValue;
    }
}
