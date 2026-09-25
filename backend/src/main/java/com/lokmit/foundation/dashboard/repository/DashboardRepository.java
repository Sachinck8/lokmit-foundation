package com.lokmit.foundation.dashboard.repository;

import com.lokmit.foundation.dashboard.dto.DashboardApplicationResponse;
import com.lokmit.foundation.dashboard.dto.DashboardEnquiryResponse;
import com.lokmit.foundation.dashboard.dto.DashboardSummaryResponse;
import com.lokmit.foundation.dashboard.dto.DashboardUserResponse;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Read-only data access for the Admin Dashboard (A2).
 *
 * <p>The dashboard spans domains that have no JPA entities yet (candidates,
 * employers, jobs and job applications exist only as V8 schema tables), so
 * this repository uses explicit, named-parameter projections via
 * {@link NamedParameterJdbcTemplate} against the existing tables. No
 * dashboard-specific tables are created, and no rows are loaded into Java
 * for counting — all aggregation happens in the database. The {@code limit}
 * parameters are validated by the service (1..10) and bound as typed
 * parameters, never concatenated into SQL.</p>
 *
 * <p>Status filters mirror the CHECK constraints exactly:
 * {@code users.status IN ('ACTIVE','LOCKED','SUSPENDED','DELETED')},
 * {@code jobs.status IN ('DRAFT','PUBLISHED','CLOSED','ARCHIVED')},
 * {@code contact_messages.status IN ('NEW','READ','REPLIED','ARCHIVED')}
 * — no status value is invented here.</p>
 */
@Repository
public class DashboardRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public DashboardRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ------------------------------------------------------------------
    // Summary counts — one lightweight COUNT per metric, DB-side aggregation
    // ------------------------------------------------------------------

    public DashboardSummaryResponse summaryCounts() {
        long totalUsers = count("SELECT COUNT(*) FROM users");
        long activeUsers = count(
                "SELECT COUNT(*) FROM users WHERE status = 'ACTIVE'");

        return DashboardSummaryResponse.builder()
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                // Inactive = every non-ACTIVE account (LOCKED + SUSPENDED +
                // DELETED), matching the A1 fail-closed status model.
                .inactiveUsers(totalUsers - activeUsers)
                .totalCandidates(count("SELECT COUNT(*) FROM candidates"))
                .totalEmployers(count("SELECT COUNT(*) FROM employers"))
                .totalJobs(count("SELECT COUNT(*) FROM jobs"))
                .publishedJobs(count(
                        "SELECT COUNT(*) FROM jobs WHERE status = 'PUBLISHED'"))
                .draftJobs(count(
                        "SELECT COUNT(*) FROM jobs WHERE status = 'DRAFT'"))
                .closedJobs(count(
                        "SELECT COUNT(*) FROM jobs WHERE status = 'CLOSED'"))
                .archivedJobs(count(
                        "SELECT COUNT(*) FROM jobs WHERE status = 'ARCHIVED'"))
                .totalApplications(count("SELECT COUNT(*) FROM job_applications"))
                .newContactEnquiries(count(
                        "SELECT COUNT(*) FROM contact_messages WHERE status = 'NEW'"))
                .readContactEnquiries(count(
                        "SELECT COUNT(*) FROM contact_messages WHERE status = 'READ'"))
                .repliedContactEnquiries(count(
                        "SELECT COUNT(*) FROM contact_messages WHERE status = 'REPLIED'"))
                .archivedContactEnquiries(count(
                        "SELECT COUNT(*) FROM contact_messages WHERE status = 'ARCHIVED'"))
                .build();
    }

    private long count(String sql) {
        Long result = jdbc.getJdbcOperations().queryForObject(sql, Long.class);
        return result != null ? result : 0L;
    }

    // ------------------------------------------------------------------
    // Recent activity projections
    // ------------------------------------------------------------------

    private static final RowMapper<DashboardEnquiryResponse> ENQUIRY_MAPPER =
            (rs, rowNum) -> DashboardEnquiryResponse.builder()
                    .id(rs.getLong("id"))
                    .name(rs.getString("name"))
                    .email(rs.getString("email"))
                    .subject(rs.getString("subject"))
                    .status(rs.getString("status"))
                    .createdAt(offsetDateTime(rs, "created_at"))
                    .build();

    private static final RowMapper<DashboardUserResponse> USER_MAPPER =
            (rs, rowNum) -> DashboardUserResponse.builder()
                    .id(rs.getLong("id"))
                    .email(rs.getString("email"))
                    .fullName(rs.getString("full_name"))
                    .userType(rs.getString("user_type"))
                    .status(rs.getString("status"))
                    .createdAt(offsetDateTime(rs, "created_at"))
                    .lastLoginAt(offsetDateTime(rs, "last_login_at"))
                    .build();

    private static final RowMapper<DashboardApplicationResponse> APPLICATION_MAPPER =
            (rs, rowNum) -> DashboardApplicationResponse.builder()
                    .id(rs.getLong("id"))
                    .status(rs.getString("status"))
                    .appliedAt(offsetDateTime(rs, "applied_at"))
                    .candidateId(rs.getLong("candidate_id"))
                    .candidateName(rs.getString("candidate_name"))
                    .jobId(rs.getLong("job_id"))
                    .jobTitle(rs.getString("job_title"))
                    .build();

    /** Newest enquiries first (limit validated by the service, 1..10). */
    public List<DashboardEnquiryResponse> recentEnquiries(int limit) {
        return jdbc.query("""
                        SELECT id, sender_name AS name, sender_email AS email,
                               subject, status, created_at
                        FROM contact_messages
                        ORDER BY created_at DESC, id DESC
                        LIMIT :limit
                        """,
                new MapSqlParameterSource("limit", limit),
                ENQUIRY_MAPPER);
    }

    /** Newest accounts first; never selects password_hash or any secret. */
    public List<DashboardUserResponse> recentUsers(int limit) {
        return jdbc.query("""
                        SELECT id, email, full_name, user_type, status, created_at, last_login_at
                        FROM users
                        ORDER BY created_at DESC, id DESC
                        LIMIT :limit
                        """,
                new MapSqlParameterSource("limit", limit),
                USER_MAPPER);
    }

    /**
     * Newest applications first, joined in a single query (no N+1):
     * candidate name via {@code candidates.user_id → users}, job title via
     * {@code job_applications.job_id → jobs}.
     */
    public List<DashboardApplicationResponse> recentApplications(int limit) {
        return jdbc.query("""
                        SELECT ja.id, ja.status, ja.applied_at,
                               c.id AS candidate_id,
                               u.full_name AS candidate_name,
                               j.id AS job_id,
                               j.title AS job_title
                        FROM job_applications ja
                        JOIN candidates c ON c.id = ja.candidate_id
                        JOIN users u ON u.id = c.user_id
                        JOIN jobs j ON j.id = ja.job_id
                        ORDER BY ja.applied_at DESC, ja.id DESC
                        LIMIT :limit
                        """,
                new MapSqlParameterSource("limit", limit),
                APPLICATION_MAPPER);
    }

    /**
     * Reads a {@code timestamptz} column as {@link OffsetDateTime}, letting
     * the JDBC driver supply the correct zone offset (no local-timezone
     * assumptions). NULL stays NULL.
     */
    private static OffsetDateTime offsetDateTime(ResultSet rs, String column) throws SQLException {
        return rs.getObject(column, OffsetDateTime.class);
    }
}
