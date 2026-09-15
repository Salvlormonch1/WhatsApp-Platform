package com.saasplatform.whatsapp.service;

import com.saasplatform.whatsapp.domain.WhatsAppIntegration;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.util.Map;

/**
 * Sends messages via WhatsApp Cloud API.
 * Decoupted from the webhook processing logic.
 * Each business uses its own access token (retrieved and decrypted per message).
 */
@ApplicationScoped
public class WhatsAppSender {

    private static final Logger LOG = Logger.getLogger(WhatsAppSender.class);

    @ConfigProperty(name = "platform.whatsapp.api-url")
    String apiUrl;

    @Inject
    EncryptionService encryptionService;

    /**
     * Send a text message to a customer.
     * Returns the WhatsApp message ID from the API response.
     */
    public String sendTextMessage(WhatsAppIntegration integration, String toPhone, String text) {
        try {
            String accessToken = encryptionService.decrypt(integration.accessTokenEncrypted);

            Map<String, Object> payload = Map.of(
                    "messaging_product", "whatsapp",
                    "to", toPhone,
                    "type", "text",
                    "text", Map.of("body", text)
            );

            Client client = ClientBuilder.newClient();
            Response response = client
                    .target(apiUrl + "/" + integration.phoneNumberId + "/messages")
                    .request(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + accessToken)
                    .post(Entity.json(payload));

            if (response.getStatus() == 200 || response.getStatus() == 201) {
                Map<?, ?> responseBody = response.readEntity(Map.class);
                Object messages = responseBody.get("messages");
                if (messages instanceof java.util.List<?> msgList && !msgList.isEmpty()) {
                    Map<?, ?> firstMsg = (Map<?, ?>) msgList.get(0);
                    return (String) firstMsg.get("id");
                }
            } else {
                LOG.errorf("WhatsApp API error %d for phone %s: %s",
                        response.getStatus(), toPhone, response.readEntity(String.class));
            }

        } catch (Exception e) {
            LOG.errorf(e, "Failed to send WhatsApp message to %s", toPhone);
        }
        return null;
    }
}
