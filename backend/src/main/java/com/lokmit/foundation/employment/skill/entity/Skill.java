package com.lokmit.foundation.employment.skill.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * Maps to the existing 'skills' table created in V8__employment_schema.sql.
 * {@code uq_skills_name} enforces unique skill names; status is constrained
 * by chk_skills_status to ACTIVE/INACTIVE.
 */
@Entity
@Table(name = "skills")
@Getter
@Setter
public class Skill {

    /** Lifecycle statuses admitted by chk_skills_status. */
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_INACTIVE = "INACTIVE";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
