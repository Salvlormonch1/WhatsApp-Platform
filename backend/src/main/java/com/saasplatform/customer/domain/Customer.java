package com.saasplatform.customer.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * A customer of a business — identified by phone number (WhatsApp primary key).
 * The same phone can exist in multiple businesses (separate tenant records).
 * Customers are NOT platform users — they interact via WhatsApp only.
 */
@Entity
@Table(name = "customers",
        uniqueConstraints = @UniqueConstraint(columnNames = {"business_id", "phone"}))
public class Customer extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    public UUID id;

    @Column(name = "business_id", nullable = false, updatable = false)
    public UUID businessId;

    @Column(name = "phone", nullable = false)
    public String phone;

    @Column(name = "name")
    public String name;

    @Column(name = "email")
    public String email;

    @Column(name = "notes", columnDefinition = "TEXT")
    public String notes;

    @Column(name = "first_contact_at", nullable = false)
    public Instant firstContactAt;

    @Column(name = "last_contact_at", nullable = false)
    public Instant lastContactAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    public Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt;

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (firstContactAt == null) firstContactAt = now;
        if (lastContactAt == null) lastContactAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }

    /** Find or return null — tenant-safe lookup by phone */
    public static Customer findByPhoneAndBusiness(String phone, UUID businessId) {
        return find("phone = ?1 AND businessId = ?2", phone, businessId).firstResult();
    }

    public static Customer findByIdAndBusiness(UUID id, UUID businessId) {
        return find("id = ?1 AND businessId = ?2", id, businessId).firstResult();
    }

    public static io.quarkus.panache.common.Page pageByBusiness(UUID businessId, int page, int size) {
        return io.quarkus.panache.common.Page.of(page, size);
    }
}
