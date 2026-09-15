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
 * Google Gemini implementation of LLMClient.
 *
 * Uses Gemini's OpenAI-compatible API endpoint — no extra SDK needed.
 * Supports: gemini-2.0-flash, gemini-1.5-pro, gemini-1.5-flash
 *
 * To enable: set platform.ai.provider=gemini in application.properties
 *
 * Gemini OpenAI-compat docs:
 *   https://ai.google.dev/gemini-api/docs/openai
 */
@ApplicationScoped
@jakarta.enterprise.inject.Typed(GeminiLLMClient.class)
public class GeminiLLMClient implements LLMClient {

    private static final Logger LOG = Logger.getLogger(GeminiLLMClient.class);

    /** Get your key at https://aistudio.google.com/apikey — it's FREE for moderate usage */
    @ConfigProperty(name = "platform.ai.gemini.api-key", defaultValue = "")
    String apiKey;

    @ConfigProperty(name = "platform.ai.gemini.model", defaultValue = "gemini-2.0-flash")
    String model;

    @ConfigProperty(name = "platform.ai.gemini.max-tokens", defaultValue = "1000")
    int maxTokens;

    @ConfigProperty(name = "platform.ai.gemini.temperature", defaultValue = "0.3")
    double temperature;

    // Gemini's OpenAI-compatible endpoint
    private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta/openai";

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
                    .target(BASE_URL + "/chat/completions")
                    .request(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + apiKey)
                    .post(Entity.json(requestBody));

            long duration = System.currentTimeMillis() - startMs;

            if (response.getStatus() != 200) {
                String error = response.readEntity(String.class);
                LOG.errorf("Gemini API error %d (business=%s, %dms): %s",
                        response.getStatus(), businessId, duration, error);
                return null;
            }

            ChatCompletionResponse result = response.readEntity(ChatCompletionResponse.class);
            LOG.debugf("Gemini response (business=%s, model=%s, tokens=%d, %dms)",
                    businessId, model,
                    result.usage() != null ? result.usage().totalTokens() : 0,
                    duration);

            return result;

        } catch (Exception e) {
            LOG.errorf(e, "Gemini client exception (business=%s)", businessId);
            return null;
        }
    }
}
