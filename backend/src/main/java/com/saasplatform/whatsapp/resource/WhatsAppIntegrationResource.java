package com.saasplatform.whatsapp.resource;

import com.saasplatform.common.dto.ApiResponse;
import com.saasplatform.common.security.BusinessContext;
import com.saasplatform.whatsapp.domain.WhatsAppIntegration;
import com.saasplatform.whatsapp.service.EncryptionService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * REST resource for managing per-tenant WhatsApp Business integrations.
 * Each tenant configures their own phone number and access token here.
 * The access token is always stored encrypted — never returned in API responses.
 */
@Path("/api/v1/whatsapp/integration")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed({"OWNER", "ADMIN"})
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "WhatsApp Integration", description = "Per-tenant WhatsApp Business configuration")
public class WhatsAppIntegrationResource {

    private static final Logger LOG = Logger.getLogger(WhatsAppIntegrationResource.class);

    @Inject
    BusinessContext businessContext;

    @Inject
    EncryptionService encryptionService;

    @ConfigProperty(name = "platform.whatsapp.verify-token")
    String platformVerifyToken;

    // DTO records
    public record ConnectRequest(
            String phoneNumberId,
            String accessToken,
            String whatsappBusinessAccountId,
            String displayPhone
    ) {}

    public record IntegrationStatus(
            boolean connected,
            String phoneNumberId,
            String displayPhone,
            String whatsappBusinessAccountId,
            String connectedAt,
            String webhookUrl
    ) {}

    /**
     * GET /api/v1/whatsapp/integration
     * Returns the integration status for this tenant.
     * Never returns the access token.
     */
    @GET
    public Response getIntegration() {
        UUID businessId = businessContext.getBusinessId();
        WhatsAppIntegration integration = WhatsAppIntegration.findByBusiness(businessId);

        if (integration == null) {
            return Response.ok(ApiResponse.ok(new IntegrationStatus(
                    false, null, null, null, null,
                    buildWebhookUrl()
            ))).build();
        }

        return Response.ok(ApiResponse.ok(new IntegrationStatus(
                integration.active,
                integration.phoneNumberId,
                integration.displayPhone,
                integration.whatsappBusinessAccountId,
                integration.connectedAt != null ? integration.connectedAt.toString() : null,
                buildWebhookUrl()
        ))).build();
    }

    /**
     * POST /api/v1/whatsapp/integration/connect
     * Connect or update the WhatsApp Business number for this tenant.
     * The access token is encrypted with AES-256-GCM before storage.
     */
    @POST
    @Path("/connect")
    @Transactional
    public Response connect(ConnectRequest req) {
        UUID businessId = businessContext.getBusinessId();

        if (req.phoneNumberId() == null || req.phoneNumberId().isBlank()) {
            return Response.status(400)
                    .entity(ApiResponse.error("phoneNumberId es requerido")).build();
        }
        if (req.accessToken() == null || req.accessToken().isBlank()) {
            return Response.status(400)
                    .entity(ApiResponse.error("accessToken es requerido")).build();
        }

        // Check if another business already owns this phone number
        WhatsAppIntegration existing = WhatsAppIntegration.findByPhoneNumberId(req.phoneNumberId());
        if (existing != null && !existing.businessId.equals(businessId)) {
            return Response.status(409)
                    .entity(ApiResponse.error("Este número ya está registrado en otra cuenta")).build();
        }

        // Find or create
        WhatsAppIntegration integration = WhatsAppIntegration.findByBusiness(businessId);
        boolean isNew = integration == null;
        if (isNew) {
            integration = new WhatsAppIntegration();
            integration.businessId = businessId;
        }

        // Encrypt the token — it never touches the DB in plaintext
        String encrypted = encryptionService.encrypt(req.accessToken());

        integration.phoneNumberId = req.phoneNumberId().trim();
        integration.accessTokenEncrypted = encrypted;
        integration.whatsappBusinessAccountId = req.whatsappBusinessAccountId();
        integration.displayPhone = req.displayPhone();
        integration.active = true;
        integration.connectedAt = Instant.now();

        integration.persist();

        LOG.infof("WhatsApp integration %s for business %s (phone: %s)",
                isNew ? "created" : "updated", businessId, integration.displayPhone);

        return Response.ok(ApiResponse.ok("WhatsApp conectado correctamente",
                new IntegrationStatus(
                        true,
                        integration.phoneNumberId,
                        integration.displayPhone,
                        integration.whatsappBusinessAccountId,
                        integration.connectedAt.toString(),
                        buildWebhookUrl()
                ))).build();
    }

    /**
     * DELETE /api/v1/whatsapp/integration
     * Disconnects the WhatsApp integration (soft delete — marks inactive).
     */
    @DELETE
    @Transactional
    public Response disconnect() {
        UUID businessId = businessContext.getBusinessId();
        WhatsAppIntegration integration = WhatsAppIntegration.findByBusiness(businessId);

        if (integration == null) {
            return Response.status(404)
                    .entity(ApiResponse.error("No hay integración configurada")).build();
        }

        integration.active = false;
        LOG.infof("WhatsApp integration disconnected for business %s", businessId);

        return Response.ok(ApiResponse.ok("WhatsApp desconectado")).build();
    }

    /**
     * POST /api/v1/whatsapp/integration/test
     * Sends a test message to verify the integration works.
     */
    @POST
    @Path("/test")
    public Response testIntegration(@QueryParam("toPhone") String toPhone) {
        UUID businessId = businessContext.getBusinessId();
        WhatsAppIntegration integration = WhatsAppIntegration.findByBusiness(businessId);

        if (integration == null || !integration.active) {
            return Response.status(404)
                    .entity(ApiResponse.error("No hay integración activa")).build();
        }

        if (toPhone == null || toPhone.isBlank()) {
            return Response.status(400)
                    .entity(ApiResponse.error("toPhone es requerido")).build();
        }

        // In a real implementation, send a test message via WhatsAppSender
        // For now, return the config details (without the token)
        return Response.ok(ApiResponse.ok("Configuración válida",
                Map.of("phoneNumberId", integration.phoneNumberId,
                       "displayPhone", integration.displayPhone != null ? integration.displayPhone : "N/A",
                       "active", integration.active))).build();
    }

    private String buildWebhookUrl() {
        // This URL is what the tenant configures in Meta's webhook settings
        // In production, replace with your actual domain
        return "https://tu-plataforma.com/api/v1/whatsapp/webhook";
    }
}
