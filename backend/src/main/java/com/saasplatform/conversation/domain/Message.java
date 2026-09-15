package com.saasplatform.conversation.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "messages")
public class Message extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    public UUID id;

    @Column(name = "conversation_id", nullable = false)
    public UUID conversationId;

    @Column(name = "business_id", nullable = false)
    public UUID businessId;

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false)
    public MessageDirection direction;

    @Enumerated(EnumType.STRING)
    @Column(name = "sender_type", nullable = false)
    public MessageSenderType senderType;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    public String content;

    @Column(name = "whatsapp_message_id")
    public String whatsappMessageId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    public MessageStatus status = MessageStatus.SENT;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    public Map<String, Object> metadata;

    @Column(name = "created_at", nullable = false, updatable = false)
    public Instant createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = Instant.now();
    }

    @SuppressWarnings("unchecked")
    public static List<Message> findByConversation(UUID conversationId, UUID businessId) {
        return (List<Message>) (List<?>) list("conversationId = ?1 AND businessId = ?2 ORDER BY createdAt ASC", conversationId, businessId);
    }

    @SuppressWarnings("unchecked")
    public static List<Message> findLastNByConversation(UUID conversationId, UUID businessId, int n) {
        return ((List<Message>) (List<?>) find("conversationId = ?1 AND businessId = ?2 ORDER BY createdAt DESC", conversationId, businessId)
                .page(0, n).list())
                .reversed();
    }

    public enum MessageDirection { INBOUND, OUTBOUND }
    public enum MessageSenderType { CUSTOMER, AI, HUMAN }
    public enum MessageStatus { SENT, DELIVERED, READ, FAILED }
}
