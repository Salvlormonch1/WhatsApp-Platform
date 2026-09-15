package com.saasplatform.ai.client;

import com.saasplatform.ai.dto.AITool;
import com.saasplatform.ai.dto.ChatCompletionResponse;
import com.saasplatform.ai.dto.ChatMessage;

import java.util.List;
import java.util.UUID;

/**
 * Provider-agnostic interface for LLM chat completions.
 *
 * Implementations:
 *  - OpenAILLMClient  → OpenAI GPT-4o, GPT-4o-mini, GPT-3.5-turbo
 *  - GeminiLLMClient  → Google Gemini 2.0 Flash, Gemini 1.5 Pro (via OpenAI-compatible API)
 *
 * To switch provider: change platform.ai.provider=openai|gemini in application.properties
 * The rest of the codebase (AIOrchestrator) never changes.
 */
public interface LLMClient {

    /**
     * Send a chat completion request with optional tool definitions.
     *
     * @param messages    Ordered message history (system, user, assistant, tool)
     * @param tools       Tool definitions for function/tool calling (may be null)
     * @param businessId  Used only for logging — never sent to the LLM provider
     * @return            The completion response, or null on error
     */
    ChatCompletionResponse chat(List<ChatMessage> messages, List<AITool> tools, UUID businessId);
}
