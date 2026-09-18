package com.lokmit.foundation.db;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * A7.6.2 integration tests for the database-level semantics the upload
 * lifecycle depends on, executed against a throwaway schema in the
 * configured development database — the same real-PostgreSQL approach as
 * {@link FlywayMigrationIntegrationTest} and the A7.6.1
 * {@link ResumeStorageMigrationIntegrationTest} (skipped cleanly when
 * PostgreSQL is not reachable; no fake database behavior).
 *
 * <p>Verifies: the deactivate-then-insert ordering satisfies
 * {@code uq_resumes_one_active_per_candidate}; two active resumes can never
 * coexist; metadata + blob commit/roll back atomically (exact validated
 * bytes stored and readable); the storage key/checksum columns round-trip;
 * and no public URL is recorded (the legacy NOT NULL file_url keeps a
 * non-dereferenceable marker).</p>
 */
class ResumeUploadLifecycleIntegrationTest {

    private static final String IT_SCHEMA = "lokmit_it_a762";

    private static final String URL = resolve("DB_URL",
            "jdbc:postgresql://localhost:5432/lokmit_foundation");
    private static final String USER = resolve("DB_USERNAME", "lokmit_app");
    private static final String PASSWORD = resolve("DB_PASSWORD", "");

    private Connection connection;
    private long candidateId;

    /**
     * ONE deterministic @BeforeEach: reachability assumption FIRST (clean
     * skip when PostgreSQL is unavailable, exactly like the existing
     * integration tests), then a fresh per-test schema migration.
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
                "PostgreSQL not reachable or credentials not configured — A7.6.2 integration tests skipped");

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
            System.err.println("Warning: could not drop " + IT_SCHEMA + ": " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // seed helpers
    // ------------------------------------------------------------------

    private void seedCandidate() throws SQLException {
        try (Statement st = connection.createStatement()) {
            st.execute("""
                    INSERT INTO users (email, password_hash, full_name, user_type, status, email_verified)
                    VALUES ('a762-candidate@example.org',
                            '$2a$10$storedhashnotusedbyjwtfilter012345678901234567890123',
                            'A762 Candidate', 'CANDIDATE', 'ACTIVE', true)
                    """);
            st.execute("""
                    INSERT INTO candidates (user_id)
                    SELECT id FROM users WHERE email = 'a762-candidate@example.org'
                    """);
        }
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT id FROM candidates WHERE user_id = "
                        + "(SELECT id FROM users WHERE email = 'a762-candidate@example.org')");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            candidateId = rs.getLong(1);
        }
    }

    private long insertResume(String fileName, boolean active, String storageKey,
                              String checksum, byte[] content) throws SQLException {
        boolean hasBlob = content != null;
        try (PreparedStatement ps = connection.prepareStatement("""
                INSERT INTO resumes (candidate_id, file_url, file_name, file_type,
                                     file_size_bytes, is_active, created_at,
                                     checksum_sha256, storage_key)
                VALUES (?, 'internal:db-blob', ?, 'application/pdf', ?, true, now(), ?, ?)
                RETURNING id
                """)) {
            ps.setLong(1, candidateId);
            ps.setString(2, fileName);
            ps.setLong(3, content != null ? content.length : 12L);
            ps.setString(4, checksum);
            ps.setString(5, storageKey);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                long id = rs.getLong(1);
                if (hasBlob) {
                    insertBlob(id, content);
                }
                return id;
            }
        }
    }

    private void insertBlob(long resumeId, byte[] content) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO resumes_file_blobs (resume_id, content) VALUES (?, ?)")) {
            ps.setLong(1, resumeId);
            ps.setBytes(2, content);
            ps.executeUpdate();
        }
    }

    private static byte[] validPdfBytes() {
        return "%PDF-1.4\nA7.6.2 integration payload\n%%EOF\n".getBytes(StandardCharsets.US_ASCII);
    }

    private static String sha256(byte[] content) {
        byte[] digest;
        try {
            digest = MessageDigest.getInstance("SHA-256").digest(content);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is mandatory on every supported JDK", e);
        }
        StringBuilder hex = new StringBuilder(digest.length * 2);
        for (byte b : digest) {
            hex.append(Character.forDigit((b >> 4) & 0xF, 16));
            hex.append(Character.forDigit(b & 0xF, 16));
        }
        return hex.toString();
    }

    // ------------------------------------------------------------------
    // tests
    // ------------------------------------------------------------------

    @Test
    @DisplayName("deactivate-then-insert keeps at most one active resume per candidate")
    void deactivateThenInsertKeepsSingleActiveResume() throws SQLException {
        seedCandidate();
        byte[] pdf = validPdfBytes();

        // First upload: insert an active resume with its blob.
        long first = insertResume("first.pdf", true, "resumes/x/first-uuid", sha256(pdf), pdf);
        assertTrue(first > 0);

        // Re-upload lifecycle step 1: deactivate the previous active row.
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE resumes SET is_active = FALSE WHERE candidate_id = ? AND is_active = TRUE")) {
            ps.setLong(1, candidateId);
            assertEquals(1, ps.executeUpdate(), "exactly the one active row is deactivated");
        }

        // Re-upload lifecycle step 2: insert the new active row — allowed.
        long second = insertResume("second.pdf", true, "resumes/x/second-uuid", sha256(pdf), pdf);
        assertTrue(second > 0);

        // Exactly one active resume remains.
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT COUNT(*) FROM resumes WHERE candidate_id = ? AND is_active = TRUE")) {
            ps.setLong(1, candidateId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                assertEquals(1, rs.getInt(1), "exactly one active resume after re-upload");
            }
        }
    }

    @Test
    @DisplayName("a second ACTIVE row for the same candidate violates the partial unique index")
    void secondActiveRowRejected() throws SQLException {
        seedCandidate();
        insertResume("one.pdf", true, "resumes/x/one-uuid", null, validPdfBytes());

        SQLException violation = assertThrows(SQLException.class,
                () -> insertResume("two.pdf", true, "resumes/x/two-uuid", null, validPdfBytes()));
        assertTrue(violation.getSQLState() != null && violation.getSQLState().startsWith("23"),
                "unique violation SQLSTATE class 23 expected, got: " + violation.getSQLState());
    }

    @Test
    @DisplayName("metadata + blob commit atomically: exact bytes stored and readable")
    void bytesRoundTripExactly() throws Exception {
        seedCandidate();
        byte[] pdf = validPdfBytes();
        String checksum = sha256(pdf);

        long resumeId = insertResume("cv.pdf", true, "resumes/" + 42 + "/roundtrip-uuid",
                checksum, pdf);

        // Read the bytes back through the storage query path (resume_id PK).
        byte[] loaded;
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT content FROM resumes_file_blobs WHERE resume_id = ?")) {
            ps.setLong(1, resumeId);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next(), "blob row must exist");
                loaded = rs.getBytes(1);
                assertFalse(rs.next(), "exactly one blob row per resume (1:1)");
            }
        }
        assertArrayEquals(pdf, loaded, "stored bytes must equal the validated bytes exactly");

        // Checksum of the STORED bytes matches the recorded checksum.
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT checksum_sha256, storage_key, file_size_bytes FROM resumes WHERE id = ?")) {
            ps.setLong(1, resumeId);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                assertEquals(checksum, rs.getString(1), "recorded checksum matches stored bytes");
                assertEquals("resumes/42/roundtrip-uuid", rs.getString(2));
                assertEquals(pdf.length, rs.getLong(3));
            }
        }
    }

    @Test
    @DisplayName("transaction rollback removes metadata AND blob (no orphan bytes)")
    void rollbackRemovesBothRows() throws SQLException {
        seedCandidate();
        byte[] pdf = validPdfBytes();
        connection.setAutoCommit(false);
        try {
            long resumeId;
            try (PreparedStatement ps = connection.prepareStatement("""
                    INSERT INTO resumes (candidate_id, file_url, file_name, file_type,
                                         file_size_bytes, is_active, created_at,
                                         checksum_sha256, storage_key)
                    VALUES (?, 'internal:db-blob', 'rollback.pdf', 'application/pdf',
                            ?, true, now(), ?, 'resumes/x/rollback-uuid')
                    RETURNING id
                    """)) {
                ps.setLong(1, candidateId);
                ps.setLong(2, pdf.length);
                ps.setString(3, sha256(pdf));
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    resumeId = rs.getLong(1);
                }
            }
            insertBlob(resumeId, pdf);

            connection.rollback();

            // Both rows are gone: no orphan metadata, no orphan bytes.
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT COUNT(*) FROM resumes WHERE id = ?")) {
                ps.setLong(1, resumeId);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    assertEquals(0, rs.getInt(1), "metadata row must be rolled back");
                }
            }
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT COUNT(*) FROM resumes_file_blobs WHERE resume_id = ?")) {
                ps.setLong(1, resumeId);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    assertEquals(0, rs.getInt(1), "blob row must be rolled back with the metadata");
                }
            }
        } finally {
            connection.setAutoCommit(true);
        }
    }

    @Test
    @DisplayName("legacy file_url stays NOT NULL and carries no public URL for new rows")
    void legacyFileUrlMarkerOnly() throws SQLException {
        seedCandidate();
        long resumeId = insertResume("marker.pdf", true, "resumes/x/marker-uuid",
                null, validPdfBytes());

        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT file_url, storage_key FROM resumes WHERE id = ?")) {
            ps.setLong(1, resumeId);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                assertEquals("internal:db-blob", rs.getString(1),
                        "legacy NOT NULL column keeps the non-dereferenceable marker");
                assertFalse(rs.getString(1).contains("http"), "no public URL is recorded");
                assertEquals("resumes/x/marker-uuid", rs.getString(2),
                        "opaque server-side storage key is recorded");
            }
        }
    }

    @Test
    @DisplayName("candidate deletion cascades resume AND blob (complete chain)")
    void candidateDeletionCascadesAll() throws SQLException {
        seedCandidate();
        long resumeId = insertResume("cascade.pdf", true, "resumes/x/cascade-uuid",
                null, validPdfBytes());

        try (Statement st = connection.createStatement()) {
            st.execute("DELETE FROM users WHERE email = 'a762-candidate@example.org'");
        }

        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT (SELECT COUNT(*) FROM resumes WHERE id = ?) AS r, "
                        + "(SELECT COUNT(*) FROM resumes_file_blobs WHERE resume_id = ?) AS b")) {
            ps.setLong(1, resumeId);
            ps.setLong(2, resumeId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                assertEquals(0, rs.getInt("r"), "resume row must cascade away with the candidate");
                assertEquals(0, rs.getInt("b"), "blob row must cascade away with the resume");
            }
        }
    }

    private static String resolve(String envKey, String fallback) {
        String value = System.getenv(envKey);
        return (value == null || value.isBlank()) ? fallback : value;
    }
}
