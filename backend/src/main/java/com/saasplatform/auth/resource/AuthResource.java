package com.saasplatform.auth.resource;

import com.saasplatform.auth.dto.AuthResponse;
import com.saasplatform.auth.dto.LoginRequest;
import com.saasplatform.auth.dto.RegisterRequest;
import com.saasplatform.auth.service.AuthService;
import com.saasplatform.common.dto.ApiResponse;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 * Authentication endpoints — public, no JWT required.
 *
 * POST /api/v1/auth/register  → create user + business, return tokens
 * POST /api/v1/auth/login     → authenticate, return tokens
 * POST /api/v1/auth/refresh   → refresh access token
 * POST /api/v1/auth/logout    → revoke refresh token
 */
@Path("/api/v1/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Authentication", description = "User registration, login, token management")
public class AuthResource {

    @Inject
    AuthService authService;

    @POST
    @Path("/register")
    @Operation(summary = "Register a new user and create their business tenant")
    public Response register(@Valid RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return Response.status(Response.Status.CREATED)
                .entity(ApiResponse.created(response))
                .build();
    }

    @POST
    @Path("/login")
    @Operation(summary = "Authenticate with email and password")
    public Response login(@Valid LoginRequest request) {
        AuthResponse response = authService.login(request);
        return Response.ok(ApiResponse.ok(response)).build();
    }

    @POST
    @Path("/refresh")
    @Operation(summary = "Refresh access token using a valid refresh token")
    public Response refresh(@QueryParam("token") String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ApiResponse.error("Refresh token required"))
                    .build();
        }
        AuthResponse response = authService.refresh(refreshToken);
        return Response.ok(ApiResponse.ok(response)).build();
    }

    @POST
    @Path("/logout")
    @Operation(summary = "Revoke refresh token (logout)")
    public Response logout(@QueryParam("token") String refreshToken) {
        authService.logout(refreshToken);
        return Response.ok(ApiResponse.ok("Logged out successfully", null)).build();
    }
}
