package com.lokmit.foundation.employment.jobcategory.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * Maps to the existing 'job_categories' table created in
 * V8__employment_schema.sql. uq_job_categories_name and
 * uq_job_categories_slug enforce uniqueness; status is constrained by
 * chk_job_categories_status to ACTIVE/INACTIVE.
 */
@Entity
@Table(name = "job_categories")
@Getter
@Setter
public class JobCategory {

    /** Lifecycle statuses admitted by chk_job_categories_status. */
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_INACTIVE = "INACTIVE";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 120)
    private String slug;

    @Column(length = 500)
    private String description;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
