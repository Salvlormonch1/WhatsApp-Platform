package com.saasplatform.business.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Business entity — the root of the tenant hierarchy.
 * Every piece of data in the system belongs to a Business.
 */
@Entity
@Table(name = "businesses")
public class Business extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    public UUID id;

    @Column(name = "name", nullable = false)
    public String name;

    @Column(name = "slug", nullable = false, unique = true)
    public String slug;

    @Enumerated(EnumType.STRING)
    @Column(name = "business_type", nullable = false)
    public BusinessType businessType = BusinessType.OTHER;

    @Column(name = "description", columnDefinition = "TEXT")
    public String description;

    @Column(name = "address", columnDefinition = "TEXT")
    public String address;

    @Column(name = "phone")
    public String phone;

    @Column(name = "email")
    public String email;

    @Column(name = "website")
    public String website;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "social_links", columnDefinition = "jsonb")
    public Map<String, String> socialLinks;

    @Column(name = "timezone", nullable = false)
    public String timezone = "UTC";

    @Column(name = "active", nullable = false)
    public boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    public Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }

    public static Business findBySlug(String slug) {
        return find("slug = ?1 AND active = true", slug).firstResult();
    }

    public static boolean slugExists(String slug) {
        return count("slug = ?1", slug) > 0;
    }
}
