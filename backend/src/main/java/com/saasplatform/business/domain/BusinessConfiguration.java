package com.saasplatform.business.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Business configuration — AI settings, notifications, booking rules.
 * One-to-one with Business.
 */
@Entity
@Table(name = "business_configurations")
public class BusinessConfiguration extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    public UUID id;

    @Column(name = "business_id", nullable = false, unique = true)
    public UUID businessId;

    @Column(name = "ai_assistant_name")
    public String aiAssistantName = "Asistente";

    @Enumerated(EnumType.STRING)
    @Column(name = "ai_tone", nullable = false)
    public AiTone aiTone = AiTone.FRIENDLY;

    @Column(name = "ai_custom_rules", columnDefinition = "TEXT")
    public String aiCustomRules;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ai_faqs", columnDefinition = "jsonb")
    public List<AiFaq> aiFaqs;

    @Column(name = "ai_escalation_triggers", columnDefinition = "TEXT[]")
    public String[] aiEscalationTriggers = {};

    @Column(name = "notification_emails", columnDefinition = "TEXT[]")
    public String[] notificationEmails = {};

    @Column(name = "weekly_report_enabled", nullable = false)
    public boolean weeklyReportEnabled = true;

    @Column(name = "booking_lead_time_minutes", nullable = false)
    public int bookingLeadTimeMinutes = 60;

    @Column(name = "booking_max_days_ahead", nullable = false)
    public int bookingMaxDaysAhead = 30;

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

    public static BusinessConfiguration findByBusinessId(UUID businessId) {
        return find("businessId = ?1", businessId).firstResult();
    }

    public enum AiTone { FORMAL, FRIENDLY, CASUAL }

    public record AiFaq(String question, String answer) {}
}
