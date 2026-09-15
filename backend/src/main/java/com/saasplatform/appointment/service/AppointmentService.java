package com.saasplatform.appointment.service;

import com.saasplatform.appointment.domain.*;
import com.saasplatform.business.domain.BusinessConfiguration;
import com.saasplatform.business.domain.BusinessHours;
import com.saasplatform.common.dto.PageResponse;
import com.saasplatform.common.exception.BadRequestException;
import com.saasplatform.common.exception.NotFoundException;
import com.saasplatform.common.security.BusinessContext;
import com.saasplatform.customer.domain.Customer;
import com.saasplatform.employee.domain.Employee;
import com.saasplatform.service.domain.Service;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Appointment service — availability computation and booking management.
 *
 * Availability algorithm:
 *  1. Determine business hours for the requested day.
 *  2. Generate candidate slots every N minutes (service duration).
 *  3. Filter out slots conflicting with existing appointments.
 *  4. Filter out slots violating lead time or max-days-ahead rules.
 *  5. Return available slots.
 *
 * The AI uses get_available_slots() tool → this service → returns list of ISO-8601 strings.
 * The AI uses create_appointment() tool → this service validates and creates → returns appointment.
 *
 * CRITICAL: No operation reads businessId from user/AI input.
 *           businessId always comes from BusinessContext (JWT).
 */
@ApplicationScoped
public class AppointmentService {

    private static final Logger LOG = Logger.getLogger(AppointmentService.class);
    private static final int SLOT_INTERVAL_MINUTES = 30;

    @Inject
    BusinessContext businessContext;

    // -------------------------------------------------------------------------
    // Availability
    // -------------------------------------------------------------------------

    /**
     * Compute available booking slots for a given date and service.
     * Used by both the dashboard and the AI tool.
     */
    public List<AvailableSlot> getAvailableSlots(LocalDate date, UUID serviceId, UUID employeeId) {
        UUID businessId = businessContext.getBusinessId();

        // Validate service belongs to this business
        Service service = Service.findByIdAndBusiness(serviceId, businessId);
        if (service == null) throw new NotFoundException("Service");

        // Get business configuration for rules
        BusinessConfiguration config = BusinessConfiguration.findByBusinessId(businessId);
        int leadTimeMinutes = config != null ? config.bookingLeadTimeMinutes : 60;
        int maxDaysAhead = config != null ? config.bookingMaxDaysAhead : 30;

        // Determine timezone (default UTC)
        String timezone = "UTC";
        com.saasplatform.business.domain.Business business =
                com.saasplatform.business.domain.Business.findById(businessId);
        if (business != null) timezone = business.timezone;

        ZoneId zoneId = ZoneId.of(timezone);
        ZonedDateTime now = ZonedDateTime.now(zoneId);

        // Validate date range
        LocalDate today = now.toLocalDate();
        if (date.isBefore(today)) {
            return Collections.emptyList();
        }
        if (date.isAfter(today.plusDays(maxDaysAhead))) {
            return Collections.emptyList();
        }

        // Get business hours for this day of week (0=Sunday)
        int dayOfWeek = date.getDayOfWeek().getValue() % 7; // Java DayOfWeek: MON=1..SUN=7 → convert to 0=Sun
        Optional<BusinessHours> hours = BusinessHours
                .findByBusiness(businessId)
                .stream()
                .filter(h -> h.dayOfWeek == dayOfWeek)
                .findFirst();

        if (hours.isEmpty() || hours.get().isClosed) {
            return Collections.emptyList();
        }

        BusinessHours bh = hours.get();
        LocalTime openTime = bh.openTime;
        LocalTime closeTime = bh.closeTime;

        // Get existing appointments that day
        Instant dayStart = date.atStartOfDay(zoneId).toInstant();
        Instant dayEnd = date.plusDays(1).atStartOfDay(zoneId).toInstant();

        List<Appointment> existing = employeeId != null
                ? Appointment.find(
                "businessId = ?1 AND employeeId = ?2 AND scheduledAt >= ?3 AND scheduledAt < ?4 AND status NOT IN ?5",
                businessId, employeeId, dayStart, dayEnd,
                List.of(AppointmentStatus.CANCELLED)).list()
                : Appointment.find(
                "businessId = ?1 AND scheduledAt >= ?2 AND scheduledAt < ?3 AND status NOT IN ?4",
                businessId, dayStart, dayEnd,
                List.of(AppointmentStatus.CANCELLED)).list();

        // Generate candidate slots
        List<AvailableSlot> available = new ArrayList<>();
        LocalTime cursor = openTime;

        while (!cursor.plusMinutes(service.durationMinutes).isAfter(closeTime)) {
            ZonedDateTime slotStart = ZonedDateTime.of(date, cursor, zoneId);
            ZonedDateTime slotEnd = slotStart.plusMinutes(service.durationMinutes);

            // Check lead time
            if (slotStart.toInstant().isAfter(now.plusMinutes(leadTimeMinutes).toInstant())) {
                // Check conflict with existing appointments
                Instant slotStartInst = slotStart.toInstant();
                Instant slotEndInst = slotEnd.toInstant();

                boolean conflict = existing.stream().anyMatch(a -> {
                    Instant aEnd = a.getEndTime();
                    return slotStartInst.isBefore(aEnd) && slotEndInst.isAfter(a.scheduledAt);
                });

                if (!conflict) {
                    available.add(new AvailableSlot(
                            slotStart.toInstant(),
                            slotEnd.toInstant(),
                            cursor.toString(),
                            slotStart.toString()
                    ));
                }
            }

            cursor = cursor.plusMinutes(SLOT_INTERVAL_MINUTES);
        }

        return available;
    }

    // -------------------------------------------------------------------------
    // CRUD
    // -------------------------------------------------------------------------

    public PageResponse<Appointment> listAppointments(
            int page, int size, AppointmentStatus status, UUID employeeId,
            LocalDate dateFrom, LocalDate dateTo) {

        UUID businessId = businessContext.getBusinessId();
        StringBuilder query = new StringBuilder("businessId = ?1");
        List<Object> params = new ArrayList<>(List.of(businessId));
        int paramIdx = 2;

        if (status != null) {
            query.append(" AND status = ?").append(paramIdx++);
            params.add(status);
        }
        if (employeeId != null) {
            query.append(" AND employeeId = ?").append(paramIdx++);
            params.add(employeeId);
        }
        if (dateFrom != null) {
            query.append(" AND scheduledAt >= ?").append(paramIdx++);
            params.add(dateFrom.atStartOfDay(ZoneOffset.UTC).toInstant());
        }
        if (dateTo != null) {
            query.append(" AND scheduledAt < ?").append(paramIdx++);
            params.add(dateTo.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant());
        }
        query.append(" ORDER BY scheduledAt DESC");

        var q = Appointment.find(query.toString(), params.toArray());
        long total = q.count();
        List<Appointment> items = q.page(page, size).list();
        return PageResponse.of(items, page, size, total);
    }

    public Appointment getAppointment(UUID id) {
        Appointment appt = Appointment.findByIdAndBusiness(id, businessContext.getBusinessId());
        if (appt == null) throw new NotFoundException("Appointment");
        return appt;
    }

    @Transactional
    public Appointment createAppointment(AppointmentRequest request) {
        UUID businessId = businessContext.getBusinessId();

        // Validate service
        Service service = Service.findByIdAndBusiness(request.serviceId(), businessId);
        if (service == null) throw new BadRequestException("Service not found or not active");

        // Validate customer
        Customer customer = Customer.findByIdAndBusiness(request.customerId(), businessId);
        if (customer == null) throw new BadRequestException("Customer not found");

        // Validate employee (optional)
        if (request.employeeId() != null) {
            Employee emp = Employee.findByIdAndBusiness(request.employeeId(), businessId);
            if (emp == null) throw new BadRequestException("Employee not found");
        }

        Appointment appt = new Appointment();
        appt.businessId = businessId;
        appt.customerId = request.customerId();
        appt.serviceId = request.serviceId();
        appt.employeeId = request.employeeId();
        appt.scheduledAt = request.scheduledAt();
        appt.durationMinutes = service.durationMinutes;
        appt.status = AppointmentStatus.CONFIRMED;
        appt.channel = request.channel() != null ? request.channel() : AppointmentChannel.MANUAL;
        appt.createdBy = request.createdBy() != null ? request.createdBy() : AppointmentCreatedBy.HUMAN;
        appt.notes = request.notes();
        appt.persist();

        LOG.infof("Appointment created: %s for customer %s at %s", appt.id, appt.customerId, appt.scheduledAt);
        return appt;
    }

    @Transactional
    public Appointment updateStatus(UUID id, AppointmentStatus newStatus, String reason) {
        Appointment appt = getAppointment(id);
        appt.status = newStatus;
        if (reason != null) appt.cancelledReason = reason;
        return appt;
    }

    @Transactional
    public Appointment reschedule(UUID id, Instant newScheduledAt) {
        Appointment appt = getAppointment(id);
        if (appt.status == AppointmentStatus.CANCELLED || appt.status == AppointmentStatus.COMPLETED) {
            throw new BadRequestException("Cannot reschedule a " + appt.status.name().toLowerCase() + " appointment");
        }
        appt.scheduledAt = newScheduledAt;
        appt.status = AppointmentStatus.CONFIRMED;
        return appt;
    }

    @Transactional
    public void cancelAppointment(UUID id, String reason) {
        Appointment appt = getAppointment(id);
        appt.status = AppointmentStatus.CANCELLED;
        appt.cancelledReason = reason;
    }

    // -------------------------------------------------------------------------
    // DTOs
    // -------------------------------------------------------------------------

    public record AppointmentRequest(
            UUID customerId,
            UUID serviceId,
            UUID employeeId,
            Instant scheduledAt,
            String notes,
            AppointmentChannel channel,
            AppointmentCreatedBy createdBy
    ) {}

    public record AvailableSlot(
            Instant startAt,
            Instant endAt,
            String localTime,
            String isoDateTime
    ) {}
}
