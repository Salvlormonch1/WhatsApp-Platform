package com.saasplatform.report.resource;

import com.saasplatform.business.domain.Business;
import com.saasplatform.common.dto.ApiResponse;
import com.saasplatform.common.security.BusinessContext;
import com.saasplatform.report.domain.Report;
import com.saasplatform.report.service.ReportService;
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

@Path("/api/v1/reports")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed({"OWNER", "ADMIN"})
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Reports", description = "Business performance reports")
public class ReportResource {

    @Inject
    ReportService reportService;

    @Inject
    BusinessContext businessContext;

    @GET
    public Response listReports(@QueryParam("limit") @DefaultValue("10") int limit) {
        UUID businessId = businessContext.getBusinessId();
        List<Report> reports = Report.findByBusiness(businessId, limit);
        return Response.ok(ApiResponse.ok(reports)).build();
    }

    @GET
    @Path("/latest")
    public Response getLatest() {
        UUID businessId = businessContext.getBusinessId();
        Report report = Report.findLatestByBusiness(businessId);
        if (report == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(ApiResponse.error("No reports found")).build();
        }
        return Response.ok(ApiResponse.ok(report)).build();
    }

    @POST
    @Path("/generate")
    @RolesAllowed({"OWNER"})
    public Response generateReport(
            @QueryParam("weekStart") String weekStartStr,
            @QueryParam("weekEnd") String weekEndStr) {

        UUID businessId = businessContext.getBusinessId();
        Business business = Business.findById(businessId);

        LocalDate weekStart = weekStartStr != null
                ? LocalDate.parse(weekStartStr)
                : LocalDate.now().minusWeeks(1).with(java.time.DayOfWeek.MONDAY);
        LocalDate weekEnd = weekEndStr != null
                ? LocalDate.parse(weekEndStr)
                : weekStart.plusDays(6);

        Report report = reportService.generateReportForBusiness(business, weekStart, weekEnd);
        return Response.status(Response.Status.CREATED).entity(ApiResponse.created(report)).build();
    }
}
