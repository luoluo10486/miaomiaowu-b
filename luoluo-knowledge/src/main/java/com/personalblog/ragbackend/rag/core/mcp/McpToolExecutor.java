package com.personalblog.ragbackend.rag.core.mcp;

import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;

import java.util.Map;

public interface McpToolExecutor {
    Tool getToolDefinition();

    CallToolResult execute(Map<String, Object> parameters);

    default String getToolId() {
        return getToolDefinition().name();
    }
}
