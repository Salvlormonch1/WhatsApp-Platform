package com.saasplatform.auth.domain;

/**
 * Roles within a business tenant.
 * Authorization is enforced server-side — never trust frontend role claims.
 */
public enum UserRole {
    /** Full control over the business, billing, team management */
    OWNER,
    /** Can manage appointments, services, conversations */
    ADMIN,
    /** Limited access — own appointments, basic operations */
    EMPLOYEE
}
