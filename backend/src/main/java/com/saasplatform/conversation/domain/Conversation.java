package com.saasplatform.conversation.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "conversations")
public class Conversation extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    public UUID id;

    @Column(name = "business_id", nullable = false, updatable = false)
    public UUID businessId;

    @Column(name = "customer_id", nullable = false)
    public UUID customerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    public ConversationStatus status = ConversationStatus.AI_ACTIVE;

    @Column(name = "assigned_to")
    public UUID assignedTo;

    @Column(name = "channel")
    public String channel = "WHATSAPP";

    @Column(name = "escalation_reason", columnDefinition = "TEXT")
    public String escalationReason;

    @Column(name = "escalated_at")
    public Instant escalatedAt;

    @Column(name = "resolved_at")
    public Instant resolvedAt;

    @Column(name = "last_message_at", nullable = false)
    public Instant lastMessageAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    public Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt;

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (lastMessageAt == null) lastMessageAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }

    // ---- Finders ----

    /** Find active (non-resolved) conversation for a customer */
    public static Conversation findActiveByCustomerAndBusiness(UUID customerId, UUID businessId) {
        return find(
            "customerId = ?1 AND businessId = ?2 AND status != ?3 ORDER BY createdAt DESC",
            customerId, businessId, ConversationStatus.RESOLVED
        ).firstResult();
    }

    public static Conversation findByIdAndBusiness(UUID id, UUID businessId) {
        return find("id = ?1 AND businessId = ?2", id, businessId).firstResult();
    }

    public static List<Conversation> findByBusiness(UUID businessId) {
        return list("businessId = ?1 ORDER BY lastMessageAt DESC", businessId);
    }

    public static long countByBusinessAndStatus(UUID businessId, ConversationStatus status) {
        return count("businessId = ?1 AND status = ?2", businessId, status);
    }
}
