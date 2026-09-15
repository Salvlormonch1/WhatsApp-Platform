package com.saasplatform.business.resource;

import com.saasplatform.business.domain.Business;
import com.saasplatform.business.domain.BusinessConfiguration;
import com.saasplatform.business.domain.BusinessHours;
import com.saasplatform.business.service.BusinessService;
import com.saasplatform.common.dto.ApiResponse;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;

/**
 * Business management endpoints — all require JWT.
 * businessId is NEVER taken from request — always from JWT via BusinessContext.
 */
@Path("/api/v1/business")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed({"OWNER", "ADMIN", "EMPLOYEE"})
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Business", description = "Business profile and configuration")
public class BusinessResource {

    @Inject
    BusinessService businessService;

    @GET
    @Operation(summary = "Get current business profile")
    public Response getBusiness() {
        Business business = businessService.getCurrentBusiness();
        return Response.ok(ApiResponse.ok(business)).build();
    }

    @PUT
    @RolesAllowed({"OWNER", "ADMIN"})
    @Operation(summary = "Update business profile")
    public Response updateBusiness(BusinessService.UpdateBusinessRequest request) {
        Business business = businessService.updateBusiness(request);
        return Response.ok(ApiResponse.ok("Business updated", business)).build();
    }

    @GET
    @Path("/configuration")
    @Operation(summary = "Get business AI and notification configuration")
    public Response getConfiguration() {
        BusinessConfiguration config = businessService.getConfiguration();
        return Response.ok(ApiResponse.ok(config)).build();
    }

    @PUT
    @Path("/configuration")
    @RolesAllowed({"OWNER", "ADMIN"})
    @Operation(summary = "Update business configuration")
    public Response updateConfiguration(BusinessConfiguration config) {
        BusinessConfiguration updated = businessService.updateConfiguration(config);
        return Response.ok(ApiResponse.ok("Configuration updated", updated)).build();
    }

    @GET
    @Path("/hours")
    @Operation(summary = "Get business hours schedule")
    public Response getHours() {
        List<BusinessHours> hours = businessService.getHours();
        return Response.ok(ApiResponse.ok(hours)).build();
    }

    @PUT
    @Path("/hours")
    @RolesAllowed({"OWNER", "ADMIN"})
    @Operation(summary = "Update business hours schedule")
    public Response updateHours(List<BusinessHours> hours) {
        List<BusinessHours> updated = businessService.updateHours(hours);
        return Response.ok(ApiResponse.ok("Hours updated", updated)).build();
    }
}
