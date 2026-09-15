package com.saasplatform.ai.dto;

import java.util.Map;

public record FunctionDef(
        String name,
        String description,
        Map<String, Object> parameters
) {}
