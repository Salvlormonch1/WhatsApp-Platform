package com.saasplatform.common.security;

import jakarta.enterprise.context.RequestScoped;

import java.util.UUID;

/**
 * CDI RequestScoped bean holding the authenticated tenant context.
 *
 * This is populated by the SecurityContextFilter at the beginning of every request.
 * All repository and service methods that access tenant data MUST use businessId
 * from this context — NEVER from request parameters or body.
 *
 * This is the primary multi-tenant isolation mechanism.
 */
@RequestScoped
public class BusinessContext {

    private UUID businessId;
    private UUID userId;
    private String userRole;
    private String userEmail;

    public UUID getBusinessId() {
        if (businessId == null) {
            throw new IllegalStateException("BusinessContext not initialized — no authenticated tenant context available");
        }
        return businessId;
    }

    public void setBusinessId(UUID businessId) {
        this.businessId = businessId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getUserRole() {
        return userRole;
    }

    public void setUserRole(String userRole) {
        this.userRole = userRole;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public boolean isInitialized() {
        return businessId != null;
    }

    public boolean hasRole(String role) {
        return role != null && role.equals(this.userRole);
    }

    public boolean hasAnyRole(String... roles) {
        for (String role : roles) {
            if (hasRole(role)) return true;
        }
        return false;
    }
}
