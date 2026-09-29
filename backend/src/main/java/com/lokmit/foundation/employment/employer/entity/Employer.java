package com.lokmit.foundation.employment.employer.entity;

import com.lokmit.foundation.security.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * Maps to the existing 'employers' table created in V8__employment_schema.sql.
 *
 * <p>The unique constraint {@code uq_employers_user} allows exactly one
 * employer profile per user; {@code fk_employers_user} references
 * {@code users(id) ON DELETE CASCADE} — which is why this API deliberately
 * exposes NO hard-delete endpoint: deleting a profile row would cascade into
 * the owning user identity (and transitively user_roles / refresh_tokens).
 * Profile lifecycle is managed through {@code status} (ACTIVE/SUSPENDED per
 * chk_employers_status) and {@code verificationStatus} (UNVERIFIED/PENDING/
 * VERIFIED/REJECTED per chk_employers_verification).</p>
 */
@Entity
@Table(name = "employers")
@Getter
@Setter
public class Employer {

    /** Verification states admitted by chk_employers_verification. */
    public static final String VERIFICATION_UNVERIFIED = "UNVERIFIED";
    public static final String VERIFICATION_PENDING = "PENDING";
    public static final String VERIFICATION_VERIFIED = "VERIFIED";
    public static final String VERIFICATION_REJECTED = "REJECTED";

    /** Lifecycle statuses admitted by chk_employers_status. */
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_SUSPENDED = "SUSPENDED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Owning user identity. LAZY so listing never joins the users table —
     * the FK id is available without initialization and the DTO exposes only
     * the id, never the user's security fields.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "company_name", nullable = false, length = 255)
    private String companyName;

    @Column(columnDefinition = "text")
    private String about;

    @Column(name = "website_url", length = 500)
    private String websiteUrl;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(name = "contact_person_name", length = 255)
    private String contactPersonName;

    @Column(name = "contact_phone", length = 50)
    private String contactPhone;

    @Column(length = 500)
    private String address;

    @Column(name = "verification_status", nullable = false, length = 20)
    private String verificationStatus;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
