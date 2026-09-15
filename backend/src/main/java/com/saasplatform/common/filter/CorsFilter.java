package com.saasplatform.common.filter;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Optional;

/**
 * Manual CORS filter — handles preflight OPTIONS and adds CORS headers to all responses.
 * CORS_ORIGINS env var supports multiple origins separated by comma.
 * Defaults to localhost:3000 for local dev.
 */
@Provider
public class CorsFilter implements ContainerRequestFilter, ContainerResponseFilter {

    @ConfigProperty(name = "cors.allowed.origins", defaultValue = "http://localhost:3000")
    String allowedOriginsRaw;

    private static final String ALLOW_METHODS = "GET, POST, PUT, PATCH, DELETE, OPTIONS";
    private static final String ALLOW_HEADERS = "Authorization, Content-Type, Accept, X-Request-ID, X-Requested-With";

    /** Return the matching origin from the request, or the first allowed origin as fallback. */
    private String resolveOrigin(ContainerRequestContext req) {
        String requestOrigin = req.getHeaderString("Origin");
        if (requestOrigin == null || requestOrigin.isBlank()) {
            return allowedOriginsRaw.split(",")[0].trim();
        }
        for (String allowed : allowedOriginsRaw.split(",")) {
            if (allowed.trim().equalsIgnoreCase(requestOrigin.trim())) {
                return requestOrigin.trim();
            }
        }
        // If no match, still return the first configured origin (browser will block — by design)
        return allowedOriginsRaw.split(",")[0].trim();
    }

    @Override
    public void filter(ContainerRequestContext req) {
        if ("OPTIONS".equalsIgnoreCase(req.getMethod())) {
            String origin = resolveOrigin(req);
            Response preflight = Response.ok()
                    .header("Access-Control-Allow-Origin",      origin)
                    .header("Access-Control-Allow-Methods",     ALLOW_METHODS)
                    .header("Access-Control-Allow-Headers",     ALLOW_HEADERS)
                    .header("Access-Control-Allow-Credentials", "true")
                    .header("Access-Control-Max-Age",           "86400")
                    .build();
            req.abortWith(preflight);
        }
    }

    @Override
    public void filter(ContainerRequestContext req, ContainerResponseContext res) {
        String origin = resolveOrigin(req);
        res.getHeaders().add("Access-Control-Allow-Origin",      origin);
        res.getHeaders().add("Access-Control-Allow-Methods",     ALLOW_METHODS);
        res.getHeaders().add("Access-Control-Allow-Headers",     ALLOW_HEADERS);
        res.getHeaders().add("Access-Control-Allow-Credentials", "true");
        res.getHeaders().add("Access-Control-Expose-Headers",    "Authorization, Content-Disposition");
    }
}
