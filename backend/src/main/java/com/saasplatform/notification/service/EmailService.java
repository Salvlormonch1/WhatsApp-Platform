package com.saasplatform.notification.service;

import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.util.List;

/**
 * Email notification service — decoupled from all business logic.
 * Replace the Quarkus Mailer implementation with any SMTP/SaaS provider.
 */
@ApplicationScoped
public class EmailService {

    private static final Logger LOG = Logger.getLogger(EmailService.class);

    @Inject
    Mailer mailer;

    @ConfigProperty(name = "platform.email.from-name", defaultValue = "SaaS Platform")
    String fromName;

    @ConfigProperty(name = "quarkus.mailer.from", defaultValue = "noreply@localhost")
    String fromAddress;

    /**
     * Send appointment confirmation email to the business owner/admin.
     */
    public void sendAppointmentConfirmation(AppointmentConfirmationData data, List<String> recipients) {
        if (recipients == null || recipients.isEmpty()) return;

        String subject = "Nueva reserva confirmada — " + data.businessName();
        String html = buildAppointmentConfirmationHtml(data);

        try {
            for (String recipient : recipients) {
                mailer.send(Mail.withHtml(recipient, subject, html));
                LOG.infof("Appointment confirmation sent to %s for business %s", recipient, data.businessName());
            }
        } catch (Exception e) {
            LOG.errorf(e, "Failed to send appointment confirmation email");
        }
    }

    /**
     * Send human escalation alert email.
     */
    public void sendEscalationAlert(EscalationAlertData data, List<String> recipients) {
        if (recipients == null || recipients.isEmpty()) return;

        String subject = "⚠️ Conversación requiere atención — " + data.businessName();
        String html = buildEscalationAlertHtml(data);

        try {
            for (String recipient : recipients) {
                mailer.send(Mail.withHtml(recipient, subject, html));
            }
        } catch (Exception e) {
            LOG.errorf(e, "Failed to send escalation alert email");
        }
    }

    /**
     * Send weekly report email.
     */
    public void sendWeeklyReport(WeeklyReportData data, List<String> recipients) {
        if (recipients == null || recipients.isEmpty()) return;

        String subject = String.format("Reporte semanal — %s (%s al %s)",
                data.businessName(), data.periodStart(), data.periodEnd());
        String html = buildWeeklyReportHtml(data);

        try {
            for (String recipient : recipients) {
                mailer.send(Mail.withHtml(recipient, subject, html));
            }
            LOG.infof("Weekly report sent to %d recipients for business %s",
                    recipients.size(), data.businessName());
        } catch (Exception e) {
            LOG.errorf(e, "Failed to send weekly report email");
        }
    }

    // -------------------------------------------------------------------------
    // HTML builders
    // -------------------------------------------------------------------------

    private String buildAppointmentConfirmationHtml(AppointmentConfirmationData d) {
        return """
                <!DOCTYPE html>
                <html lang="es">
                <head><meta charset="UTF-8"><style>
                  body{font-family:Arial,sans-serif;background:#f5f5f5;margin:0;padding:0}
                  .container{max-width:600px;margin:40px auto;background:#fff;border-radius:12px;overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,.1)}
                  .header{background:linear-gradient(135deg,#6366f1,#8b5cf6);color:#fff;padding:32px;text-align:center}
                  .header h1{margin:0;font-size:24px}
                  .body{padding:32px}
                  .field{margin-bottom:16px}
                  .label{font-size:12px;color:#6b7280;text-transform:uppercase;letter-spacing:.05em}
                  .value{font-size:16px;color:#111827;font-weight:600;margin-top:4px}
                  .btn{display:inline-block;background:#6366f1;color:#fff;padding:12px 28px;border-radius:8px;text-decoration:none;font-weight:600;margin-top:24px}
                  .footer{background:#f9fafb;padding:16px 32px;text-align:center;color:#9ca3af;font-size:12px}
                </style></head>
                <body>
                <div class="container">
                  <div class="header"><h1>✅ Nueva Reserva Confirmada</h1><p>%s</p></div>
                  <div class="body">
                    <div class="field"><div class="label">Cliente</div><div class="value">%s</div></div>
                    <div class="field"><div class="label">Servicio</div><div class="value">%s</div></div>
                    <div class="field"><div class="label">Fecha y hora</div><div class="value">%s</div></div>
                    <div class="field"><div class="label">Teléfono</div><div class="value">%s</div></div>
                    %s
                  </div>
                  <div class="footer">%s — Sistema de reservas</div>
                </div>
                </body></html>
                """.formatted(
                d.businessName(), d.customerName(), d.serviceName(),
                d.scheduledAt(), d.customerPhone(),
                d.employeeName() != null ? "<div class=\"field\"><div class=\"label\">Empleado</div><div class=\"value\">" + d.employeeName() + "</div></div>" : "",
                d.businessName()
        );
    }

    private String buildEscalationAlertHtml(EscalationAlertData d) {
        return """
                <!DOCTYPE html>
                <html lang="es">
                <head><meta charset="UTF-8"><style>
                  body{font-family:Arial,sans-serif;background:#f5f5f5}
                  .container{max-width:600px;margin:40px auto;background:#fff;border-radius:12px;overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,.1)}
                  .header{background:linear-gradient(135deg,#ef4444,#f97316);color:#fff;padding:32px;text-align:center}
                  .body{padding:32px}
                  .field{margin-bottom:16px}
                  .label{font-size:12px;color:#6b7280;text-transform:uppercase}
                  .value{font-size:16px;color:#111827;font-weight:600;margin-top:4px}
                  .reason{background:#fef2f2;border:1px solid #fecaca;border-radius:8px;padding:16px;margin:16px 0}
                </style></head>
                <body>
                <div class="container">
                  <div class="header"><h1>⚠️ Atención Requerida</h1></div>
                  <div class="body">
                    <p>Una conversación requiere intervención humana.</p>
                    <div class="field"><div class="label">Cliente</div><div class="value">%s (%s)</div></div>
                    <div class="reason"><strong>Motivo:</strong><br>%s</div>
                  </div>
                </div>
                </body></html>
                """.formatted(d.customerName(), d.customerPhone(), d.reason());
    }

    private String buildWeeklyReportHtml(WeeklyReportData d) {
        return """
                <!DOCTYPE html>
                <html lang="es">
                <head><meta charset="UTF-8"><style>
                  body{font-family:Arial,sans-serif;background:#f5f5f5}
                  .container{max-width:600px;margin:40px auto;background:#fff;border-radius:12px;overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,.1)}
                  .header{background:linear-gradient(135deg,#6366f1,#8b5cf6);color:#fff;padding:32px;text-align:center}
                  .body{padding:32px}
                  .stat-grid{display:grid;grid-template-columns:1fr 1fr;gap:16px;margin:24px 0}
                  .stat{background:#f9fafb;border-radius:8px;padding:16px;text-align:center}
                  .stat-value{font-size:32px;font-weight:700;color:#6366f1}
                  .stat-label{font-size:12px;color:#6b7280;text-transform:uppercase;margin-top:4px}
                  h3{color:#374151;border-bottom:1px solid #e5e7eb;padding-bottom:8px}
                </style></head>
                <body>
                <div class="container">
                  <div class="header">
                    <h1>📊 Reporte Semanal</h1>
                    <p>%s</p>
                    <small>%s — %s</small>
                  </div>
                  <div class="body">
                    <h3>Reservas</h3>
                    <div class="stat-grid">
                      <div class="stat"><div class="stat-value">%d</div><div class="stat-label">Total</div></div>
                      <div class="stat"><div class="stat-value">%d</div><div class="stat-label">Confirmadas</div></div>
                      <div class="stat"><div class="stat-value">%d</div><div class="stat-label">Canceladas</div></div>
                      <div class="stat"><div class="stat-value">%d</div><div class="stat-label">Completadas</div></div>
                    </div>
                    <h3>Clientes</h3>
                    <div class="stat-grid">
                      <div class="stat"><div class="stat-value">%d</div><div class="stat-label">Nuevos</div></div>
                      <div class="stat"><div class="stat-value">%d</div><div class="stat-label">Conversaciones IA</div></div>
                    </div>
                    <h3>IA</h3>
                    <div class="stat-grid">
                      <div class="stat"><div class="stat-value">%d</div><div class="stat-label">Escaladas</div></div>
                      <div class="stat"><div class="stat-value">%s%%</div><div class="stat-label">Tasa escalación</div></div>
                    </div>
                    %s
                  </div>
                </div>
                </body></html>
                """.formatted(
                d.businessName(), d.periodStart(), d.periodEnd(),
                d.totalAppointments(), d.confirmedAppointments(),
                d.cancelledAppointments(), d.completedAppointments(),
                d.newCustomers(), d.totalConversations(),
                d.escalatedConversations(), d.escalationRate(),
                d.topService() != null ? "<h3>Servicio más solicitado</h3><p><strong>" + d.topService() + "</strong></p>" : ""
        );
    }

    // -------------------------------------------------------------------------
    // Data records
    // -------------------------------------------------------------------------

    public record AppointmentConfirmationData(
            String businessName, String customerName, String customerPhone,
            String serviceName, String scheduledAt, String employeeName) {}

    public record EscalationAlertData(
            String businessName, String customerName, String customerPhone, String reason) {}

    public record WeeklyReportData(
            String businessName, String periodStart, String periodEnd,
            int totalAppointments, int confirmedAppointments, int cancelledAppointments,
            int completedAppointments, int newCustomers, int totalConversations,
            int escalatedConversations, String escalationRate, String topService) {}
}
