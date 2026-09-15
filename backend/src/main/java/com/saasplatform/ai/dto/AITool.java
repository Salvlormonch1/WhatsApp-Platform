package com.saasplatform.ai.dto;

public record AITool(
        String type,
        FunctionDef function
) {
    public AITool(FunctionDef function) {
        this("function", function);
    }
}
