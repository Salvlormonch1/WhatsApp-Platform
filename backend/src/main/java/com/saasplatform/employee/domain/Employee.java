package com.saasplatform.employee.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "employees")
public class Employee extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    public UUID id;

    @Column(name = "business_id", nullable = false, updatable = false)
    public UUID businessId;

    @Column(name = "user_id")
    public UUID userId;

    @Column(name = "name", nullable = false)
    public String name;

    @Column(name = "phone")
    public String phone;

    @Column(name = "email")
    public String email;

    @Column(name = "bio", columnDefinition = "TEXT")
    public String bio;

    @Column(name = "avatar_url")
    public String avatarUrl;

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

    public static List<Employee> findByBusiness(UUID businessId) {
        return list("businessId = ?1 AND active = true ORDER BY name", businessId);
    }

    public static Employee findByIdAndBusiness(UUID id, UUID businessId) {
        return find("id = ?1 AND businessId = ?2 AND active = true", id, businessId).firstResult();
    }
}
