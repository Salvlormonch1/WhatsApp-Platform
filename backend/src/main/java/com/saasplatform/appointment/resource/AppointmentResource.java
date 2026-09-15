package com.saasplatform.appointment.resource;

import com.saasplatform.appointment.domain.Appointment;
import com.saasplatform.appointment.domain.AppointmentStatus;
import com.saasplatform.appointment.service.AppointmentService;
import com.saasplatform.common.dto.ApiResponse;
import com.saasplatform.common.dto.PageResponse;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Path("/api/v1/appointments")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed({"OWNER", "ADMIN", "EMPLOYEE"})
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Appointments", description = "Appointment booking and management")
public class AppointmentResource {

    @Inject
    AppointmentService appointmentService;

    @GET
    public Response list(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size,
            @QueryParam("status") AppointmentStatus status,
            @QueryParam("employeeId") UUID employeeId,
            @QueryParam("dateFrom") String dateFrom,
            @QueryParam("dateTo") String dateTo) {

        LocalDate from = dateFrom != null ? LocalDate.parse(dateFrom) : null;
        LocalDate to = dateTo != null ? LocalDate.parse(dateTo) : null;

        PageResponse<Appointment> result = appointmentService.listAppointments(page, size, status, employeeId, from, to);
        return Response.ok(ApiResponse.ok(result)).build();
    }

    @GET
    @Path("/availability")
    public Response availability(
            @QueryParam("date") String date,
            @QueryParam("serviceId") UUID serviceId,
            @QueryParam("employeeId") UUID employeeId) {

        if (date == null || serviceId == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ApiResponse.error("date and serviceId are required"))
                    .build();
        }

        List<AppointmentService.AvailableSlot> slots =
                appointmentService.getAvailableSlots(LocalDate.parse(date), serviceId, employeeId);
        return Response.ok(ApiResponse.ok(slots)).build();
    }

    @GET
    @Path("/{id}")
    public Response get(@PathParam("id") UUID id) {
        return Response.ok(ApiResponse.ok(appointmentService.getAppointment(id))).build();
    }

    @POST
    public Response create(AppointmentService.AppointmentRequest request) {
        Appointment appt = appointmentService.createAppointment(request);
        return Response.status(Response.Status.CREATED).entity(ApiResponse.created(appt)).build();
    }

    @PATCH
    @Path("/{id}/status")
    @RolesAllowed({"OWNER", "ADMIN"})
    public Response updateStatus(@PathParam("id") UUID id, StatusUpdateRequest request) {
        Appointment appt = appointmentService.updateStatus(id, request.status(), request.reason());
        return Response.ok(ApiResponse.ok("Status updated", appt)).build();
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed({"OWNER", "ADMIN"})
    public Response cancel(@PathParam("id") UUID id, @QueryParam("reason") String reason) {
        appointmentService.cancelAppointment(id, reason);
        return Response.ok(ApiResponse.ok("Appointment cancelled", null)).build();
    }

    public record StatusUpdateRequest(AppointmentStatus status, String reason) {}
}
