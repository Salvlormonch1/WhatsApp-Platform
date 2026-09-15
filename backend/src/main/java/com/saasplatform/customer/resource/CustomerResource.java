package com.saasplatform.customer.resource;

import com.saasplatform.common.dto.ApiResponse;
import com.saasplatform.common.dto.PageResponse;
import com.saasplatform.customer.domain.Customer;
import com.saasplatform.customer.service.CustomerService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.UUID;

@Path("/api/v1/customers")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed({"OWNER", "ADMIN", "EMPLOYEE"})
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Customers", description = "Customer management")
public class CustomerResource {

    @Inject
    CustomerService customerService;

    @GET
    public Response list(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size,
            @QueryParam("search") String search) {
        PageResponse<Customer> result = customerService.listCustomers(page, size, search);
        return Response.ok(ApiResponse.ok(result)).build();
    }

    @GET
    @Path("/{id}")
    public Response get(@PathParam("id") UUID id) {
        return Response.ok(ApiResponse.ok(customerService.getCustomer(id))).build();
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed({"OWNER", "ADMIN"})
    public Response update(@PathParam("id") UUID id, CustomerService.CustomerUpdateRequest request) {
        return Response.ok(ApiResponse.ok(customerService.updateCustomer(id, request))).build();
    }
}
