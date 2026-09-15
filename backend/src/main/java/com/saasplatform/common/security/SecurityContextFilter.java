package com.saasplatform.common.security;

import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;
import org.jboss.logging.MDC;

import java.io.IOException;
import java.util.UUID;

/**
 * JAX-RS filter that extracts JWT claims and populates BusinessContext.
 * Runs on every request AFTER JWT verification by Quarkus security layer.
 *
 * Also sets MDC fields for structured logging:
 *   - requestId
 *   - businessId
 *   - userId
 */
@Provider
public class SecurityContextFilter implements ContainerRequestFilter {

    private static final Logger LOG = Logger.getLogger(SecurityContextFilter.class);

    @Inject
    JsonWebToken jwt;

    @Inject
    BusinessContext businessContext;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        // Generate correlation ID for this request
        String requestId = java.util.UUID.randomUUID().toString().substring(0, 8);
        MDC.put("requestId", requestId);
        requestContext.getHeaders().add("X-Request-ID", requestId);

        // If there's no JWT (unauthenticated endpoints), skip
        if (jwt == null || jwt.getRawToken() == null) {
            return;
        }

        try {
            // Extract claims from JWT
            String businessIdClaim = jwt.getClaim("businessId");
            String userIdClaim = jwt.getSubject();
            String role = jwt.getClaim("role");
            String email = jwt.getClaim("email");

            if (businessIdClaim != null) {
                UUID businessId = UUID.fromString(businessIdClaim);
                businessContext.setBusinessId(businessId);
                businessContext.setUserId(UUID.fromString(userIdClaim));
                businessContext.setUserRole(role);
                businessContext.setUserEmail(email);

                // Add to MDC for all log statements in this request
                MDC.put("businessId", businessIdClaim);
                MDC.put("userId", userIdClaim);
            }
        } catch (Exception e) {
            LOG.warnf("Failed to extract security context from JWT: %s", e.getMessage());
        }
    }
}
