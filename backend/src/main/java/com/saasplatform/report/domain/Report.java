package com.saasplatform.report.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "reports")
public class Report extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    public UUID id;

    @Column(name = "business_id", nullable = false)
    public UUID businessId;

    @Column(name = "report_type", nullable = false)
    public String reportType = "WEEKLY";

    @Column(name = "period_start", nullable = false)
    public LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    public LocalDate periodEnd;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "data", nullable = false, columnDefinition = "jsonb")
    public Map<String, Object> data;

    @Column(name = "sent_at")
    public Instant sentAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    public Instant createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = Instant.now();
    }

    public static List<Report> findByBusiness(UUID businessId, int limit) {
        return find("businessId = ?1 ORDER BY periodStart DESC", businessId)
                .page(0, limit).list();
    }

    public static Report findLatestByBusiness(UUID businessId) {
        return find("businessId = ?1 ORDER BY periodStart DESC", businessId)
                .firstResult();
    }
}
