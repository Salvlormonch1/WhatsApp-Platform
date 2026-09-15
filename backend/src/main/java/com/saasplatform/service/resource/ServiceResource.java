package com.saasplatform.service.resource;

import com.saasplatform.common.dto.ApiResponse;
import com.saasplatform.service.domain.Service;
import com.saasplatform.service.service.ServiceCatalogService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;
import java.util.UUID;

@Path("/api/v1/services")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed({"OWNER", "ADMIN", "EMPLOYEE"})
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Services", description = "Business service catalog management")
public class ServiceResource {

    @Inject
    ServiceCatalogService serviceCatalogService;

    @GET
    public Response list() {
        List<Service> services = serviceCatalogService.listServices();
        return Response.ok(ApiResponse.ok(services)).build();
    }

    @GET
    @Path("/{id}")
    public Response get(@PathParam("id") UUID id) {
        return Response.ok(ApiResponse.ok(serviceCatalogService.getService(id))).build();
    }

    @POST
    @RolesAllowed({"OWNER", "ADMIN"})
    public Response create(ServiceCatalogService.ServiceRequest request) {
        Service service = serviceCatalogService.createService(request);
        return Response.status(Response.Status.CREATED).entity(ApiResponse.created(service)).build();
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed({"OWNER", "ADMIN"})
    public Response update(@PathParam("id") UUID id, ServiceCatalogService.ServiceRequest request) {
        return Response.ok(ApiResponse.ok(serviceCatalogService.updateService(id, request))).build();
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed({"OWNER", "ADMIN"})
    public Response delete(@PathParam("id") UUID id) {
        serviceCatalogService.deleteService(id);
        return Response.ok(ApiResponse.ok("Service deleted", null)).build();
    }
}
