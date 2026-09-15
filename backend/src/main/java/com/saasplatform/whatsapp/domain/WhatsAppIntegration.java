package com.saasplatform.whatsapp.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * WhatsApp Business integration per tenant.
 * access_token is stored ENCRYPTED — never in plaintext.
 * phone_number_id is the key used to identify the tenant from incoming webhooks.
 */
@Entity
@Table(name = "whatsapp_integrations")
public class WhatsAppIntegration extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    public UUID id;

    @Column(name = "business_id", nullable = false, unique = true)
    public UUID businessId;

    /** Meta's phone_number_id — used for webhook tenant identification */
    @Column(name = "phone_number_id", nullable = false, unique = true)
    public String phoneNumberId;

    @Column(name = "whatsapp_business_account_id")
    public String whatsappBusinessAccountId;

    /** AES-256-GCM encrypted access token */
    @Column(name = "access_token_encrypted", nullable = false, columnDefinition = "TEXT")
    public String accessTokenEncrypted;

    @Column(name = "display_phone")
    public String displayPhone;

    @Column(name = "active", nullable = false)
    public boolean active = true;

    @Column(name = "connected_at")
    public Instant connectedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    public Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
        if (connectedAt == null) connectedAt = Instant.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }

    public static WhatsAppIntegration findByPhoneNumberId(String phoneNumberId) {
        return find("phoneNumberId = ?1 AND active = true", phoneNumberId).firstResult();
    }

    public static WhatsAppIntegration findByBusiness(UUID businessId) {
        return find("businessId = ?1", businessId).firstResult();
    }
}
