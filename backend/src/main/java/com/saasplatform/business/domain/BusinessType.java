package com.saasplatform.business.domain;

/**
 * Type of business — used for display and potential feature flags.
 * Business logic must NOT branch on this in the core — it's only for configuration/UX.
 */
public enum BusinessType {
    BARBERSHOP,
    HAIR_SALON,
    BEAUTY_SALON,
    NAIL_SALON,
    SPA,
    OTHER
}
