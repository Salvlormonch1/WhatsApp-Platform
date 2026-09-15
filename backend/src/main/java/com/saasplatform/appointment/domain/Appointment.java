package com.saasplatform.appointment.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "appointments")
public class Appointment extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    public UUID id;

    @Column(name = "business_id", nullable = false, updatable = false)
    public UUID businessId;

    @Column(name = "customer_id", nullable = false)
    public UUID customerId;

    @Column(name = "service_id", nullable = false)
    public UUID serviceId;

    @Column(name = "employee_id")
    public UUID employeeId;

    @Column(name = "scheduled_at", nullable = false)
    public Instant scheduledAt;

    @Column(name = "duration_minutes", nullable = false)
    public int durationMinutes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    public AppointmentStatus status = AppointmentStatus.PENDING;

    @Column(name = "notes", columnDefinition = "TEXT")
    public String notes;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false)
    public AppointmentChannel channel = AppointmentChannel.WHATSAPP;

    @Enumerated(EnumType.STRING)
    @Column(name = "created_by", nullable = false)
    public AppointmentCreatedBy createdBy = AppointmentCreatedBy.AI;

    @Column(name = "cancelled_reason", columnDefinition = "TEXT")
    public String cancelledReason;

    @Column(name = "confirmation_sent_at")
    public Instant confirmationSentAt;

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

    public Instant getEndTime() {
        return scheduledAt.plusSeconds((long) durationMinutes * 60);
    }

    // ---- Finders ----

    public static List<Appointment> findByBusinessAndDate(UUID businessId, Instant start, Instant end) {
        return list(
            "businessId = ?1 AND scheduledAt >= ?2 AND scheduledAt < ?3 AND status NOT IN ?4 ORDER BY scheduledAt",
            businessId, start, end,
            List.of(AppointmentStatus.CANCELLED)
        );
    }

    public static List<Appointment> findByBusinessAndCustomer(UUID businessId, UUID customerId) {
        return list("businessId = ?1 AND customerId = ?2 ORDER BY scheduledAt DESC", businessId, customerId);
    }

    public static Appointment findByIdAndBusiness(UUID id, UUID businessId) {
        return find("id = ?1 AND businessId = ?2", id, businessId).firstResult();
    }

    /** Check if any appointment overlaps with the proposed slot for an employee */
    public static boolean hasConflict(UUID businessId, UUID employeeId, Instant start, Instant end, UUID excludeId) {
        if (excludeId != null) {
            return count(
                "businessId = ?1 AND employeeId = ?2 AND id != ?3 AND status NOT IN ?4 AND scheduledAt < ?5 AND (scheduledAt + CAST(durationMinutes || ' minutes' AS interval)) > ?6",
                businessId, employeeId, excludeId, List.of(AppointmentStatus.CANCELLED), end, start
            ) > 0;
        }
        return count(
            "businessId = ?1 AND employeeId = ?2 AND status NOT IN ?3 AND scheduledAt < ?4 AND (scheduledAt + CAST(durationMinutes || ' minutes' AS interval)) > ?5",
            businessId, employeeId, List.of(AppointmentStatus.CANCELLED), end, start
        ) > 0;
    }
}
