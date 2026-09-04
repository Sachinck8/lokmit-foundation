package com.lokmit.foundation.security.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Maps to the existing 'users' table created in V2__identity_schema.sql.
 * The password_hash column is excluded from Lombok's toString to prevent accidental logging.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    @Column(name = "user_type", nullable = false, length = 20)
    private String userType;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "email_verified", nullable = false)
    private Boolean emailVerified;

    @Column(name = "last_login_at")
    private OffsetDateTime lastLoginAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    @Override
    public String toString() {
        return "User{id=" + id + ", email='" + email + "', fullName='" + fullName + "', userType='" + userType + "', status='" + status + "'}";
    }
}