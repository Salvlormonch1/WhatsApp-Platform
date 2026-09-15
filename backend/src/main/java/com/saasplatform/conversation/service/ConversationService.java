package com.saasplatform.conversation.service;

import com.saasplatform.common.exception.BadRequestException;
import com.saasplatform.common.exception.NotFoundException;
import com.saasplatform.common.security.BusinessContext;
import com.saasplatform.conversation.domain.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Conversation management service.
 * Handles conversation lifecycle: creation, escalation, takeover, release.
 */
@ApplicationScoped
public class ConversationService {

    private static final Logger LOG = Logger.getLogger(ConversationService.class);

    @Inject
    BusinessContext businessContext;

    public List<Conversation> listConversations() {
        return Conversation.findByBusiness(businessContext.getBusinessId());
    }

    public Conversation getConversation(UUID id) {
        Conversation conv = Conversation.findByIdAndBusiness(id, businessContext.getBusinessId());
        if (conv == null) throw new NotFoundException("Conversation");
        return conv;
    }

    public List<Message> getMessages(UUID conversationId) {
        UUID businessId = businessContext.getBusinessId();
        // Validate conversation belongs to this business
        Conversation conv = Conversation.findByIdAndBusiness(conversationId, businessId);
        if (conv == null) throw new NotFoundException("Conversation");
        return Message.findByConversation(conversationId, businessId);
    }

    /** Find or create an active conversation for a customer (used by WhatsApp webhook). */
    @Transactional
    public Conversation findOrCreateActive(UUID customerId, UUID businessId) {
        Conversation existing = Conversation.findActiveByCustomerAndBusiness(customerId, businessId);
        if (existing != null) return existing;

        Conversation conv = new Conversation();
        conv.businessId = businessId;
        conv.customerId = customerId;
        conv.status = ConversationStatus.AI_ACTIVE;
        conv.persist();
        LOG.debugf("Created new conversation %s for customer %s", conv.id, customerId);
        return conv;
    }

    /** Save an inbound message from WhatsApp. */
    @Transactional
    public Message saveInboundMessage(UUID conversationId, UUID businessId,
                                       String content, String whatsappMessageId) {
        Message msg = new Message();
        msg.conversationId = conversationId;
        msg.businessId = businessId;
        msg.direction = Message.MessageDirection.INBOUND;
        msg.senderType = Message.MessageSenderType.CUSTOMER;
        msg.content = content;
        msg.whatsappMessageId = whatsappMessageId;
        msg.status = Message.MessageStatus.READ;
        msg.persist();

        Conversation.update("lastMessageAt = ?1, updatedAt = ?2 WHERE id = ?3",
                Instant.now(), Instant.now(), conversationId);
        return msg;
    }

    /** Save an outbound message (AI or human). */
    @Transactional
    public Message saveOutboundMessage(UUID conversationId, UUID businessId,
                                        String content, Message.MessageSenderType senderType,
                                        String whatsappMessageId) {
        Message msg = new Message();
        msg.conversationId = conversationId;
        msg.businessId = businessId;
        msg.direction = Message.MessageDirection.OUTBOUND;
        msg.senderType = senderType;
        msg.content = content;
        msg.whatsappMessageId = whatsappMessageId;
        msg.status = Message.MessageStatus.SENT;
        msg.persist();

        Conversation.update("lastMessageAt = ?1, updatedAt = ?2 WHERE id = ?3",
                Instant.now(), Instant.now(), conversationId);
        return msg;
    }

    /** Escalate conversation to human — called by AI tool. */
    @Transactional
    public Conversation escalateToHuman(UUID conversationId, UUID businessId, String reason) {
        Conversation conv = Conversation.find("id = ?1 AND businessId = ?2", conversationId, businessId)
                .firstResult();
        if (conv == null) throw new NotFoundException("Conversation");

        conv.status = ConversationStatus.WAITING_HUMAN;
        conv.escalationReason = reason;
        conv.escalatedAt = Instant.now();

        LOG.infof("Conversation %s escalated to human: %s", conversationId, reason);
        return conv;
    }

    /** Human takes control of a conversation. */
    @Transactional
    public Conversation takeOver(UUID id) {
        Conversation conv = getConversation(id);
        if (conv.status == ConversationStatus.RESOLVED) {
            throw new BadRequestException("Conversation is already resolved");
        }
        conv.status = ConversationStatus.HUMAN_ACTIVE;
        conv.assignedTo = businessContext.getUserId();
        return conv;
    }

    /** Human releases conversation back to AI. */
    @Transactional
    public Conversation releaseToAi(UUID id) {
        Conversation conv = getConversation(id);
        conv.status = ConversationStatus.AI_ACTIVE;
        conv.assignedTo = null;
        return conv;
    }

    /** Resolve a conversation. */
    @Transactional
    public Conversation resolve(UUID id) {
        Conversation conv = getConversation(id);
        conv.status = ConversationStatus.RESOLVED;
        conv.resolvedAt = Instant.now();
        conv.assignedTo = null;
        return conv;
    }

    /** Send a human reply. */
    @Transactional
    public Message sendHumanReply(UUID conversationId, String content) {
        UUID businessId = businessContext.getBusinessId();
        Conversation conv = getConversation(conversationId);

        if (conv.status == ConversationStatus.AI_ACTIVE) {
            conv.status = ConversationStatus.HUMAN_ACTIVE;
            conv.assignedTo = businessContext.getUserId();
        }

        return saveOutboundMessage(conversationId, businessId, content,
                Message.MessageSenderType.HUMAN, null);
    }
}
