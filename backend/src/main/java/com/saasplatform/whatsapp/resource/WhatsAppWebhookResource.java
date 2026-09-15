package com.saasplatform.whatsapp.resource;

import com.saasplatform.common.dto.ApiResponse;
import com.saasplatform.whatsapp.service.WhatsAppMessageProcessor;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Map;

/**
 * WhatsApp Webhook endpoint.
 * GET  — Meta webhook verification (challenge handshake)
 * POST — Inbound messages and status updates
 *
 * Security:
 * - GET is verified by matching the hub.verify_token against configured value
 * - POST signature validation (HMAC-SHA256) is done in WhatsAppWebhookFilter
 * - The endpoint returns 200 immediately; processing is async
 */
@Path("/api/v1/whatsapp/webhook")
@Tag(name = "WhatsApp Webhook", description = "Meta WhatsApp Cloud API webhook")
public class WhatsAppWebhookResource {

    private static final Logger LOG = Logger.getLogger(WhatsAppWebhookResource.class);

    @ConfigProperty(name = "platform.whatsapp.verify-token")
    String verifyToken;

    @Inject
    WhatsAppMessageProcessor messageProcessor;

    /**
     * Webhook verification — Meta sends this to validate the endpoint.
     */
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response verify(
            @QueryParam("hub.mode") String mode,
            @QueryParam("hub.verify_token") String token,
            @QueryParam("hub.challenge") String challenge) {

        if ("subscribe".equals(mode) && verifyToken.equals(token)) {
            LOG.info("WhatsApp webhook verified successfully");
            return Response.ok(challenge).build();
        }

        LOG.warnf("WhatsApp webhook verification failed — token mismatch");
        return Response.status(Response.Status.FORBIDDEN).build();
    }

    /**
     * Process inbound webhook payload from Meta.
     * Returns 200 immediately; processing happens asynchronously.
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response receive(Map<String, Object> payload) {
        try {
            processWebhookPayload(payload);
        } catch (Exception e) {
            LOG.errorf(e, "Error processing webhook payload");
            // Always return 200 to Meta to prevent retries
        }
        return Response.ok().build();
    }

    @SuppressWarnings("unchecked")
    private void processWebhookPayload(Map<String, Object> payload) {
        Object entries = payload.get("entry");
        if (!(entries instanceof List<?> entryList)) return;

        for (Object entry : entryList) {
            if (!(entry instanceof Map<?, ?> entryMap)) continue;

            Object changes = entryMap.get("changes");
            if (!(changes instanceof List<?> changeList)) continue;

            for (Object change : changeList) {
                if (!(change instanceof Map<?, ?> changeMap)) continue;
                if (!"messages".equals(changeMap.get("field"))) continue;

                Map<?, ?> value = (Map<?, ?>) changeMap.get("value");
                if (value == null) continue;

                // Extract phone_number_id (identifies the tenant)
                Map<?, ?> metadata = (Map<?, ?>) value.get("metadata");
                if (metadata == null) continue;
                String phoneNumberId = (String) metadata.get("phone_number_id");

                // Process each message
                Object messages = value.get("messages");
                if (!(messages instanceof List<?> msgList)) continue;

                for (Object message : msgList) {
                    if (!(message instanceof Map<?, ?> msgMap)) continue;

                    String type = (String) msgMap.get("type");
                    if (!"text".equals(type)) {
                        LOG.debugf("Skipping non-text message type: %s", type);
                        continue;
                    }

                    String from = (String) msgMap.get("from");
                    String waMessageId = (String) msgMap.get("id");
                    Map<?, ?> textObj = (Map<?, ?>) msgMap.get("text");
                    String body = textObj != null ? (String) textObj.get("body") : null;

                    if (from != null && body != null && phoneNumberId != null) {
                        LOG.debugf("Processing message from %s to %s: %s", from, phoneNumberId, body);
                        // Process asynchronously to return 200 fast
                        messageProcessor.processInboundMessage(phoneNumberId, from, body, waMessageId);
                    }
                }
            }
        }
    }
}
