package com.saasplatform.employee.resource;

import com.saasplatform.common.dto.ApiResponse;
import com.saasplatform.employee.domain.Employee;
import com.saasplatform.employee.service.EmployeeService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.UUID;

@Path("/api/v1/employees")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed({"OWNER", "ADMIN", "EMPLOYEE"})
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Employees", description = "Team member management")
public class EmployeeResource {

    @Inject
    EmployeeService employeeService;

    @GET
    public Response list() {
        return Response.ok(ApiResponse.ok(employeeService.listEmployees())).build();
    }

    @GET
    @Path("/{id}")
    public Response get(@PathParam("id") UUID id) {
        return Response.ok(ApiResponse.ok(employeeService.getEmployee(id))).build();
    }

    @POST
    @RolesAllowed({"OWNER", "ADMIN"})
    public Response create(EmployeeService.EmployeeRequest request) {
        Employee emp = employeeService.createEmployee(request);
        return Response.status(Response.Status.CREATED).entity(ApiResponse.created(emp)).build();
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed({"OWNER", "ADMIN"})
    public Response update(@PathParam("id") UUID id, EmployeeService.EmployeeRequest request) {
        return Response.ok(ApiResponse.ok(employeeService.updateEmployee(id, request))).build();
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed({"OWNER", "ADMIN"})
    public Response delete(@PathParam("id") UUID id) {
        employeeService.deleteEmployee(id);
        return Response.ok(ApiResponse.ok("Employee removed", null)).build();
    }
}
