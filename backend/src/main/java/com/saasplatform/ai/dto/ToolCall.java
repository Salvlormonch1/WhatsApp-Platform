package com.saasplatform.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ToolCall(
        String id,
        String type,
        FunctionCall function
) {}
