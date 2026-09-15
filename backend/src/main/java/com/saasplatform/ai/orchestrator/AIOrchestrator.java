package com.saasplatform.ai.orchestrator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saasplatform.ai.client.OpenAIClient;
import com.saasplatform.ai.dto.*;
import com.saasplatform.ai.tools.AIToolExecutor;
import com.saasplatform.business.domain.Business;
import com.saasplatform.business.domain.BusinessConfiguration;
import com.saasplatform.business.domain.BusinessHours;
import com.saasplatform.conversation.domain.Conversation;
import com.saasplatform.conversation.domain.Message;
import com.saasplatform.conversation.service.ConversationService;
import com.saasplatform.customer.domain.Customer;
import com.saasplatform.service.domain.Service;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AI Orchestrator — manages the LLM + tool calling loop.
 *
 * Architecture:
 *  1. Build system prompt from business configuration (tenant-scoped)
 *  2. Build message history from recent conversation messages
 *  3. Call LLM with available tools
 *  4. If LLM requests a tool call → backend validates + executes → result sent back
 *  5. Loop until LLM produces a final text response
 *
 * Security guarantees:
 *  - businessId is NEVER passed to LLM — it's only used internally by tool executor
 *  - LLM context contains only current business info, no cross-tenant data
 *  - All tool executions are validated by AIToolExecutor
 *  - Prompt injection protection: business data is schema-structured, not freeform
 */
@ApplicationScoped
public class AIOrchestrator {

    private static final Logger LOG = Logger.getLogger(AIOrchestrator.class);
    private static final int MAX_TOOL_ITERATIONS = 5;

    @ConfigProperty(name = "platform.ai.max-history-messages", defaultValue = "20")
    int maxHistoryMessages;

    @Inject
    OpenAIClient openAIClient;

    @Inject
    AIToolExecutor toolExecutor;

    @Inject
    ConversationService conversationService;

    @Inject
    ObjectMapper objectMapper;

    /**
     * Process a customer message through the AI pipeline.
     *
     * @param conversation  Current conversation
     * @param customer      The customer
     * @param businessId    Current tenant (used internally, never sent to LLM)
     * @param userMessage   The inbound message text
     * @return AI-generated reply text
     */
    public String process(Conversation conversation, Customer customer,
                           UUID businessId, String userMessage) {

        // Load business context
        Business business = Business.findById(businessId);
        if (business == null) return null;

        BusinessConfiguration config = BusinessConfiguration.findByBusinessId(businessId);

        // Build system prompt
        String systemPrompt = buildSystemPrompt(business, config, customer);

        // Build message history (recent N messages)
        List<Message> history = Message.findLastNByConversation(conversation.id, businessId, maxHistoryMessages);

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessage("system", systemPrompt));

        // Add history
        for (Message msg : history) {
            String role = msg.senderType == Message.MessageSenderType.CUSTOMER ? "user" : "assistant";
            messages.add(new ChatMessage(role, msg.content));
        }

        // Add current message (already saved as inbound, just add to context)
        // The last history message IS the current message, but if history is empty:
        if (history.isEmpty() || !history.getLast().content.equals(userMessage)) {
            messages.add(new ChatMessage("user", userMessage));
        }

        // Get available tools
        List<AITool> tools = toolExecutor.getToolDefinitions();

        // Tool calling loop
        int iterations = 0;
        while (iterations < MAX_TOOL_ITERATIONS) {
            iterations++;

            ChatCompletionResponse response = openAIClient.chat(messages, tools, businessId);

            if (response == null) {
                LOG.error("OpenAI returned null response");
                return "Lo siento, ocurrió un error al procesar tu mensaje. Por favor intenta nuevamente.";
            }

            ChatChoice choice = response.choices().get(0);

            // If finish_reason is "tool_calls" → execute tools
            if ("tool_calls".equals(choice.finishReason()) && choice.message().toolCalls() != null) {
                // Add assistant tool-call message to context
                messages.add(choice.message());

                for (ToolCall toolCall : choice.message().toolCalls()) {
                    LOG.debugf("AI calling tool: %s with args: %s",
                            toolCall.function().name(), toolCall.function().arguments());

                    // Execute tool (all validation in AIToolExecutor)
                    String toolResult = toolExecutor.execute(
                            toolCall.function().name(),
                            toolCall.function().arguments(),
                            businessId,
                            conversation.id,
                            customer
                    );

                    // Handle escalation
                    if ("ESCALATE".equals(toolResult)) {
                        conversationService.escalateToHuman(conversation.id, businessId,
                                extractEscalationReason(toolCall.function().arguments()));
                        return null; // AI stops responding
                    }

                    // Add tool result to context
                    messages.add(new ChatMessage("tool", toolResult, toolCall.id()));
                }
            } else {
                // Final text response
                String reply = choice.message().content();
                LOG.debugf("AI final reply (len=%d): %s...",
                        reply != null ? reply.length() : 0,
                        reply != null && reply.length() > 50 ? reply.substring(0, 50) : reply);
                return reply;
            }
        }

        LOG.warnf("AI tool loop exceeded max iterations (%d)", MAX_TOOL_ITERATIONS);
        return "Lo siento, no pude procesar tu solicitud en este momento.";
    }

    // -------------------------------------------------------------------------
    // System prompt builder
    // -------------------------------------------------------------------------

    private String buildSystemPrompt(Business business, BusinessConfiguration config, Customer customer) {
        String assistantName = config != null ? config.aiAssistantName : "Asistente";
        String tone = config != null ? config.aiTone.name() : "FRIENDLY";
        String customRules = config != null && config.aiCustomRules != null ? config.aiCustomRules : "";

        // Build hours summary
        List<BusinessHours> hours = BusinessHours.findByBusiness(business.id);
        String hoursText = hours.stream()
                .filter(h -> !h.isClosed)
                .map(h -> dayName(h.dayOfWeek) + ": " + h.openTime + " - " + h.closeTime)
                .collect(Collectors.joining(", "));

        // Current date/time for context
        ZoneId zone = ZoneId.of(business.timezone != null ? business.timezone : "UTC");
        String currentDateTime = ZonedDateTime.now(zone)
                .format(DateTimeFormatter.ofPattern("EEEE dd 'de' MMMM yyyy, HH:mm", Locale.forLanguageTag("es")));

        String customerName = customer.name != null ? customer.name : "cliente";

        return String.format("""
                Eres %s, el asistente virtual de %s.
                
                INFORMACIÓN DEL NEGOCIO:
                - Nombre: %s
                - Descripción: %s
                - Dirección: %s
                - Teléfono: %s
                - Horarios: %s
                
                CLIENTE ACTUAL:
                - Nombre: %s
                - Teléfono: %s
                
                FECHA Y HORA ACTUAL: %s
                
                TONO: %s
                
                INSTRUCCIONES:
                - Responde siempre en español, de forma clara y concisa.
                - Para consultar servicios disponibles, usa la herramienta get_services.
                - Para consultar disponibilidad, usa get_available_slots (siempre proporciona fecha en formato YYYY-MM-DD).
                - Para crear una reserva, usa create_appointment (solo después de confirmar con el cliente).
                - Para cancelar una reserva, usa cancel_appointment.
                - Para escalar a un humano cuando no puedas resolver la solicitud, usa escalate_to_human.
                - NUNCA inventes disponibilidad, precios o reservas.
                - NUNCA confirmes una operación que el backend no haya confirmado exitosamente.
                - Una reserva solo está confirmada cuando la herramienta create_appointment devuelve éxito.
                - No respondas preguntas sobre otros negocios.
                - No reveles información de otros clientes.
                
                REGLAS ADICIONALES DEL NEGOCIO:
                %s
                """,
                assistantName, business.name,
                business.name,
                business.description != null ? business.description : "",
                business.address != null ? business.address : "",
                business.phone != null ? business.phone : "",
                hoursText.isEmpty() ? "Consultar directamente" : hoursText,
                customerName,
                customer.phone,
                currentDateTime,
                toneDescription(tone),
                customRules
        );
    }

    private String toneDescription(String tone) {
        return switch (tone) {
            case "FORMAL" -> "Formal y profesional. Usa usted.";
            case "CASUAL" -> "Casual y relajado. Usa tú.";
            default -> "Amigable y cordial. Usa tú pero mantén un tono respetuoso.";
        };
    }

    private String dayName(int dayOfWeek) {
        return switch (dayOfWeek) {
            case 0 -> "Domingo";
            case 1 -> "Lunes";
            case 2 -> "Martes";
            case 3 -> "Miércoles";
            case 4 -> "Jueves";
            case 5 -> "Viernes";
            case 6 -> "Sábado";
            default -> "?";
        };
    }

    private String extractEscalationReason(String arguments) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> args = objectMapper.readValue(arguments, Map.class);
            Object reason = args.getOrDefault("reason", "Requiere atención humana");
            return reason instanceof String s ? s : "Requiere atención humana";
        } catch (Exception e) {
            return "Requiere atención humana";
        }
    }
}
