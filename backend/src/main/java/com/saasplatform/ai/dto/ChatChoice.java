package com.saasplatform.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ChatChoice(
        int index,
        ChatMessage message,
        @JsonProperty("finish_reason") String finishReason
) {}
