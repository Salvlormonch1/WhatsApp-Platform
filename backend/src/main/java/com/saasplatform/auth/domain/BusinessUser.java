package com.saasplatform.auth.domain;

import com.saasplatform.business.domain.Business;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.util.UUID;

/**
 * Join table between User and Business with role.
 * Enables a user to be part of multiple businesses with different roles.
 */
@Entity
@Table(name = "business_users",
        uniqueConstraints = @UniqueConstraint(columnNames = {"business_id", "user_id"}))
public class BusinessUser extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    public UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_id", nullable = false)
    public Business business;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    public User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    public UserRole role = UserRole.EMPLOYEE;

    @Column(name = "active", nullable = false)
    public boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    public java.time.Instant createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = java.time.Instant.now();
    }

    public static BusinessUser findByUserAndBusiness(UUID userId, UUID businessId) {
        return find("user.id = ?1 AND business.id = ?2 AND active = true", userId, businessId).firstResult();
    }

    public static java.util.List<BusinessUser> findByUser(UUID userId) {
        return list("user.id = ?1 AND active = true", userId);
    }
}
