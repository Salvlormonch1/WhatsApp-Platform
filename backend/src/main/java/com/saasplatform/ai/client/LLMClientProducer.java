package com.saasplatform.ai.client;

import com.saasplatform.ai.dto.AITool;
import com.saasplatform.ai.dto.ChatCompletionResponse;
import com.saasplatform.ai.dto.ChatMessage;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.UUID;

/**
 * CDI producer that selects the correct LLMClient implementation at startup.
 *
 * Change platform.ai.provider in application.properties:
 *   platform.ai.provider=openai    → uses OpenAILLMClient (default)
 *   platform.ai.provider=gemini    → uses GeminiLLMClient (FREE tier available)
 *   platform.ai.provider=groq      → uses OpenAILLMClient pointed at Groq (fastest)
 *
 * Inject LLMClient anywhere — it's always the right implementation:
 *   @Inject LLMClient llm;
 */
@ApplicationScoped
public class LLMClientProducer {

    private static final Logger LOG = Logger.getLogger(LLMClientProducer.class);

    @ConfigProperty(name = "platform.ai.provider", defaultValue = "openai")
    String provider;

    @Inject
    OpenAIClient openAIClient;

    @Inject
    GeminiLLMClient geminiClient;

    @Produces
    @ApplicationScoped
    public LLMClient produce() {
        return switch (provider.toLowerCase()) {
            case "gemini" -> {
                LOG.infof("LLM provider: Gemini");
                yield geminiClient;
            }
            case "groq" -> {
                LOG.infof("LLM provider: Groq (via OpenAI-compatible endpoint)");
                yield openAIClient; // Groq is OpenAI-compatible — just change base-url + api-key
            }
            default -> {
                LOG.infof("LLM provider: OpenAI");
                yield openAIClient;
            }
        };
    }
}
