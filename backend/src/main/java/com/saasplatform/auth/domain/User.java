package com.saasplatform.auth.domain;

import com.saasplatform.common.domain.BaseEntity;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

/**
 * Platform user account.
 * A user can belong to multiple businesses via BusinessUser.
 * Passwords are never stored in plaintext — always bcrypt hashed.
 */
@Entity
@Table(name = "users")
public class User extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    public java.util.UUID id;

    @Column(name = "email", nullable = false, unique = true)
    public String email;

    @Column(name = "password_hash", nullable = false)
    public String passwordHash;

    @Column(name = "first_name", nullable = false)
    public String firstName;

    @Column(name = "last_name", nullable = false)
    public String lastName;

    @Column(name = "phone")
    public String phone;

    @Column(name = "avatar_url")
    public String avatarUrl;

    @Column(name = "email_verified", nullable = false)
    public boolean emailVerified = false;

    @Column(name = "active", nullable = false)
    public boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    public java.time.Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    public java.time.Instant updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = java.time.Instant.now();
        updatedAt = java.time.Instant.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = java.time.Instant.now();
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    // Panache finder methods
    public static User findByEmail(String email) {
        return find("email = ?1 AND active = true", email).firstResult();
    }
}
