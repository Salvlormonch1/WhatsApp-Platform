package com.saasplatform.ai.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saasplatform.ai.dto.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * OpenAI Chat Completions implementation of LLMClient.
 *
 * Works with any OpenAI-compatible endpoint:
 *   - OpenAI:       https://api.openai.com/v1   (default)
 *   - Groq:         https://api.groq.com/openai/v1
 *   - Together AI:  https://api.together.xyz/v1
 *   - Ollama local: http://localhost:11434/v1
 *
 * Configure: platform.ai.openai.base-url in application.properties
 */
@ApplicationScoped
@jakarta.enterprise.inject.Typed(OpenAIClient.class)
public class OpenAIClient implements LLMClient {

    private static final Logger LOG = Logger.getLogger(OpenAIClient.class);

    @ConfigProperty(name = "platform.ai.openai.api-key")
    String apiKey;

    @ConfigProperty(name = "platform.ai.openai.base-url", defaultValue = "https://api.openai.com/v1")
    String baseUrl;

    @ConfigProperty(name = "platform.ai.openai.model", defaultValue = "gpt-4o-mini")
    String model;

    @ConfigProperty(name = "platform.ai.openai.max-tokens", defaultValue = "1000")
    int maxTokens;

    @ConfigProperty(name = "platform.ai.openai.temperature", defaultValue = "0.3")
    double temperature;

    @Inject
    ObjectMapper objectMapper;

    @Override
    public ChatCompletionResponse chat(List<ChatMessage> messages, List<AITool> tools, UUID businessId) {
        long startMs = System.currentTimeMillis();

        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", messages);
            requestBody.put("max_tokens", maxTokens);
            requestBody.put("temperature", temperature);

            if (tools != null && !tools.isEmpty()) {
                requestBody.put("tools", tools);
                requestBody.put("tool_choice", "auto");
            }

            Client client = ClientBuilder.newClient();
            Response response = client
                    .target(baseUrl + "/chat/completions")
                    .request(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + apiKey)
                    .post(Entity.json(requestBody));

            long duration = System.currentTimeMillis() - startMs;

            if (response.getStatus() != 200) {
                String error = response.readEntity(String.class);
                LOG.errorf("LLM API error %d (provider=openai, business=%s, %dms): %s",
                        response.getStatus(), businessId, duration, error);
                return null;
            }

            ChatCompletionResponse result = response.readEntity(ChatCompletionResponse.class);
            LOG.debugf("LLM response (provider=openai, business=%s, model=%s, tokens=%d, %dms)",
                    businessId, model,
                    result.usage() != null ? result.usage().totalTokens() : 0,
                    duration);

            return result;

        } catch (Exception e) {
            LOG.errorf(e, "OpenAI client exception (business=%s)", businessId);
            return null;
        }
    }
}
