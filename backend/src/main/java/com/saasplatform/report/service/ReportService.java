package com.saasplatform.report.service;

import com.saasplatform.appointment.domain.Appointment;
import com.saasplatform.appointment.domain.AppointmentStatus;
import com.saasplatform.business.domain.Business;
import com.saasplatform.business.domain.BusinessConfiguration;
import com.saasplatform.conversation.domain.Conversation;
import com.saasplatform.conversation.domain.ConversationStatus;
import com.saasplatform.customer.domain.Customer;
import com.saasplatform.notification.service.EmailService;
import com.saasplatform.report.domain.Report;
import com.saasplatform.service.domain.Service;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Weekly report generation service.
 * Scheduled to run every Monday at 8:00 AM UTC.
 * Generates a report per active business and sends it by email.
 */
@ApplicationScoped
public class ReportService {

    private static final Logger LOG = Logger.getLogger(ReportService.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Inject
    EmailService emailService;

    /**
     * Scheduled job — runs every Monday at 8:00 AM UTC.
     * Generates reports for all active businesses with weekly reports enabled.
     */
    @Scheduled(cron = "{platform.scheduler.weekly-report-cron}")
    public void generateWeeklyReports() {
        LOG.info("Starting weekly report generation for all active businesses");

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(1);
        LocalDate weekEnd = weekStart.plusDays(6);

        List<Business> businesses = Business.list("active = true");
        int generated = 0;

        for (Business business : businesses) {
            try {
                generateReportForBusiness(business, weekStart, weekEnd);
                generated++;
            } catch (Exception e) {
                LOG.errorf(e, "Failed to generate weekly report for business %s", business.id);
            }
        }

        LOG.infof("Weekly reports generated: %d/%d businesses", generated, businesses.size());
    }

    /**
     * Generate a report for a specific business and period.
     * Can also be triggered manually via the API.
     */
    @Transactional
    public Report generateReportForBusiness(Business business, LocalDate weekStart, LocalDate weekEnd) {
        BusinessConfiguration config = BusinessConfiguration.findByBusinessId(business.id);

        Instant start = weekStart.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant end = weekEnd.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        // ---- Appointments ----
        List<Appointment> appointments = Appointment.find(
                "businessId = ?1 AND scheduledAt >= ?2 AND scheduledAt < ?3",
                business.id, start, end).list();

        int totalAppts = appointments.size();
        int confirmed = (int) appointments.stream().filter(a -> a.status == AppointmentStatus.CONFIRMED).count();
        int cancelled = (int) appointments.stream().filter(a -> a.status == AppointmentStatus.CANCELLED).count();
        int completed = (int) appointments.stream().filter(a -> a.status == AppointmentStatus.COMPLETED).count();
        int noShow   = (int) appointments.stream().filter(a -> a.status == AppointmentStatus.NO_SHOW).count();

        // ---- New customers ----
        long newCustomers = Customer.count(
                "businessId = ?1 AND firstContactAt >= ?2 AND firstContactAt < ?3",
                business.id, start, end);

        // ---- Conversations ----
        List<Conversation> conversations = Conversation.find(
                "businessId = ?1 AND createdAt >= ?2 AND createdAt < ?3",
                business.id, start, end).list();

        int totalConvs = conversations.size();
        int escalated = (int) conversations.stream()
                .filter(c -> c.escalatedAt != null).count();
        String escalationRate = totalConvs > 0
                ? String.format("%.1f", (double) escalated / totalConvs * 100)
                : "0.0";

        // ---- Top service ----
        String topService = appointments.stream()
                .collect(Collectors.groupingBy(a -> a.serviceId, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(e -> {
                    Service s = Service.findById(e.getKey());
                    return s != null ? s.name : "N/A";
                })
                .orElse(null);

        // ---- Build report data ----
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("appointments", Map.of(
                "total", totalAppts, "confirmed", confirmed,
                "cancelled", cancelled, "completed", completed, "no_show", noShow));
        data.put("customers", Map.of("new", newCustomers));
        data.put("conversations", Map.of(
                "total", totalConvs, "escalated", escalated, "escalation_rate", escalationRate));
        data.put("top_service", topService);

        // ---- Persist report ----
        Report report = new Report();
        report.businessId = business.id;
        report.reportType = "WEEKLY";
        report.periodStart = weekStart;
        report.periodEnd = weekEnd;
        report.data = data;
        report.persist();

        // ---- Send email (if enabled) ----
        if (config != null && config.weeklyReportEnabled &&
                config.notificationEmails != null && config.notificationEmails.length > 0) {

            emailService.sendWeeklyReport(
                    new EmailService.WeeklyReportData(
                            business.name,
                            weekStart.format(DATE_FMT),
                            weekEnd.format(DATE_FMT),
                            totalAppts, confirmed, cancelled, completed,
                            (int) newCustomers, totalConvs, escalated,
                            escalationRate, topService
                    ),
                    Arrays.asList(config.notificationEmails)
            );

            report.sentAt = Instant.now();
        }

        LOG.infof("Weekly report generated for business %s (%s - %s): %d appts, %d convs",
                business.name, weekStart, weekEnd, totalAppts, totalConvs);

        return report;
    }
}
