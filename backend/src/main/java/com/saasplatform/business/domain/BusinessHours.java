package com.saasplatform.business.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * Business hours per day of week.
 * day_of_week: 0=Sunday, 1=Monday, ..., 6=Saturday
 */
@Entity
@Table(name = "business_hours",
        uniqueConstraints = @UniqueConstraint(columnNames = {"business_id", "day_of_week"}))
public class BusinessHours extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    public UUID id;

    @Column(name = "business_id", nullable = false)
    public UUID businessId;

    @Column(name = "day_of_week", nullable = false)
    public short dayOfWeek;

    @Column(name = "open_time")
    public LocalTime openTime;

    @Column(name = "close_time")
    public LocalTime closeTime;

    @Column(name = "is_closed", nullable = false)
    public boolean isClosed = false;

    public static List<BusinessHours> findByBusiness(UUID businessId) {
        return list("businessId = ?1 ORDER BY dayOfWeek", businessId);
    }
}
