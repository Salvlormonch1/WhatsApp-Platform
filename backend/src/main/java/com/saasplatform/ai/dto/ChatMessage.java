package com.saasplatform.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ChatMessage(
        String role,
        String content,
        @JsonProperty("tool_calls") List<ToolCall> toolCalls,
        @JsonProperty("tool_call_id") String toolCallId
) {
    public ChatMessage(String role, String content) {
        this(role, content, null, null);
    }

    public ChatMessage(String role, String content, String toolCallId) {
        this(role, content, null, toolCallId);
    }
}
