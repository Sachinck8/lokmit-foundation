package com.lokmit.foundation.employment.candidate.entity;

import com.lokmit.foundation.security.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Maps to the existing 'candidates' table created in V8__employment_schema.sql.
 *
 * <p>{@code uq_candidates_user} allows exactly one candidate profile per
 * user; {@code fk_candidates_user} references {@code users(id) ON DELETE
 * CASCADE} — so this API deliberately exposes NO hard-delete endpoint, the
 * same identity-safety rationale as for Employer.</p>
 *
 * <p>{@code availability_status} is constrained by
 * chk_candidates_availability to ACTIVELY_LOOKING / OPEN_TO_OFFERS /
 * NOT_LOOKING. There is no DELETED state in the schema and none is invented
 * here.</p>
 */
@Entity
@Table(name = "candidates")
@Getter
@Setter
public class Candidate {

    /** Availability states admitted by chk_candidates_availability. */
    public static final String AVAILABILITY_ACTIVELY_LOOKING = "ACTIVELY_LOOKING";
    public static final String AVAILABILITY_OPEN_TO_OFFERS = "OPEN_TO_OFFERS";
    public static final String AVAILABILITY_NOT_LOOKING = "NOT_LOOKING";
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    /** Constrained by chk_candidates_gender if present; MALE/FEMALE/OTHER. */
    @Column(length = 30)
    private String gender;

    @Column(length = 50)
    private String phone;

    @Column(name = "current_location", length = 255)
    private String currentLocation;

    @Column(columnDefinition = "text")
    private String summary;

    @Column(name = "expected_salary_min", precision = 12, scale = 2)
    private BigDecimal expectedSalaryMin;

    @Column(name = "expected_salary_max", precision = 12, scale = 2)
    private BigDecimal expectedSalaryMax;

    @Column(name = "availability_status", nullable = false, length = 30)
    private String availabilityStatus;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
