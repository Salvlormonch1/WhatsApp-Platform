package com.saasplatform.whatsapp.service;

import com.saasplatform.appointment.domain.*;
import com.saasplatform.appointment.service.AppointmentService;
import com.saasplatform.business.domain.Business;
import com.saasplatform.business.domain.BusinessConfiguration;
import com.saasplatform.business.domain.BusinessHours;
import com.saasplatform.common.security.BusinessContext;
import com.saasplatform.conversation.domain.*;
import com.saasplatform.conversation.service.ConversationService;
import com.saasplatform.customer.domain.Customer;
import com.saasplatform.customer.service.CustomerService;
import com.saasplatform.service.domain.Service;
import com.saasplatform.whatsapp.domain.WhatsAppIntegration;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Processes inbound WhatsApp messages.
 * Identifies tenant → finds/creates customer & conversation → dispatches to AI orchestrator.
 *
 * This is the entry point for all inbound WhatsApp traffic.
 * businessId is ALWAYS derived from the WhatsApp integration lookup (phone_number_id), never from the message.
 */
@ApplicationScoped
public class WhatsAppMessageProcessor {

    private static final Logger LOG = Logger.getLogger(WhatsAppMessageProcessor.class);

    @Inject
    CustomerService customerService;

    @Inject
    ConversationService conversationService;

    @Inject
    BusinessContext businessContext;

    @Inject
    com.saasplatform.ai.orchestrator.AIOrchestrator aiOrchestrator;

    @Inject
    WhatsAppSender whatsAppSender;

    /**
     * Process an inbound WhatsApp text message.
     * Called asynchronously after the webhook returns 200 OK.
     *
     * @param phoneNumberId  Meta's phone_number_id (identifies the business)
     * @param fromPhone      Customer's phone number
     * @param messageText    Message content
     * @param waMessageId    WhatsApp message ID
     */
    @Transactional
    public void processInboundMessage(String phoneNumberId, String fromPhone,
                                       String messageText, String waMessageId) {
        try {
            // 1. Identify business from WhatsApp integration
            WhatsAppIntegration integration = WhatsAppIntegration.findByPhoneNumberId(phoneNumberId);
            if (integration == null) {
                LOG.warnf("No integration found for phone_number_id: %s", phoneNumberId);
                return;
            }
            UUID businessId = integration.businessId;

            // 2. Find or create customer (tenant-safe)
            Customer customer = customerService.findOrCreateByPhone(fromPhone, businessId, null);

            // 3. Find or create active conversation
            Conversation conversation = conversationService.findOrCreateActive(customer.id, businessId);

            // 4. Save inbound message
            conversationService.saveInboundMessage(conversation.id, businessId, messageText, waMessageId);

            // 5. If AI is not active, skip (human is handling)
            if (conversation.status != ConversationStatus.AI_ACTIVE) {
                LOG.infof("Conversation %s is in status %s — skipping AI", conversation.id, conversation.status);
                return;
            }

            // 6. Dispatch to AI orchestrator
            String aiReply = aiOrchestrator.process(conversation, customer, businessId, messageText);

            if (aiReply != null && !aiReply.isBlank()) {
                // 7. Send reply via WhatsApp
                String sentWaId = whatsAppSender.sendTextMessage(integration, fromPhone, aiReply);

                // 8. Save outbound message
                conversationService.saveOutboundMessage(
                        conversation.id, businessId, aiReply,
                        Message.MessageSenderType.AI, sentWaId
                );
            }

        } catch (Exception e) {
            LOG.errorf(e, "Error processing WhatsApp message from %s", fromPhone);
        }
    }
}
