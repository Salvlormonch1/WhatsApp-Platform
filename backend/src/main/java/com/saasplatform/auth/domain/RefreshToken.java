package com.saasplatform.auth.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Persisted refresh token.
 * Only the SHA-256 hash is stored — raw token is only transmitted to client once.
 */
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    public UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    public User user;

    @Column(name = "business_id", nullable = false)
    public UUID businessId;

    @Column(name = "token_hash", nullable = false, unique = true)
    public String tokenHash;

    @Column(name = "expires_at", nullable = false)
    public Instant expiresAt;

    @Column(name = "revoked", nullable = false)
    public boolean revoked = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    public Instant createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = Instant.now();
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isValid() {
        return !revoked && !isExpired();
    }

    public static RefreshToken findByTokenHash(String tokenHash) {
        return find("tokenHash = ?1 AND revoked = false", tokenHash).firstResult();
    }

    public static void revokeAllForUser(UUID userId) {
        update("revoked = true WHERE user.id = ?1 AND revoked = false", userId);
    }
}
