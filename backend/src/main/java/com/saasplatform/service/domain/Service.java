package com.saasplatform.service.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * A service offered by a business (e.g., haircut, beard trim).
 * Tenant-scoped via businessId.
 */
@Entity
@Table(name = "services")
public class Service extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    public UUID id;

    @Column(name = "business_id", nullable = false, updatable = false)
    public UUID businessId;

    @Column(name = "name", nullable = false)
    public String name;

    @Column(name = "description", columnDefinition = "TEXT")
    public String description;

    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    public BigDecimal price;

    @Column(name = "duration_minutes", nullable = false)
    public int durationMinutes = 30;

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

    public static List<Service> findByBusiness(UUID businessId) {
        return list("businessId = ?1 AND active = true ORDER BY name", businessId);
    }

    public static Service findByIdAndBusiness(UUID id, UUID businessId) {
        return find("id = ?1 AND businessId = ?2", id, businessId).firstResult();
    }
}
