package com.saasplatform.common.domain;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

import java.util.UUID;

/**
 * Base entity for all tenant-scoped entities.
 * Every subclass MUST have business_id set before persistence.
 * The business_id is never set from user input — always from the authenticated JWT context.
 */
@MappedSuperclass
public abstract class TenantBaseEntity extends BaseEntity {

    @Column(name = "business_id", nullable = false, updatable = false)
    public UUID businessId;
}
