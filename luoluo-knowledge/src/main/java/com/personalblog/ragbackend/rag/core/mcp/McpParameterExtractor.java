package com.personalblog.ragbackend.rag.core.mcp;

import io.modelcontextprotocol.spec.McpSchema.Tool;

import java.util.Map;

/**
 * MCPParameter提取器接口
 */
public interface McpParameterExtractor {
    Map<String, Object> extractParameters(String userQuestion, Tool tool);

    default Map<String, Object> extractParameters(String userQuestion, Tool tool, String customPromptTemplate) {
        return extractParameters(userQuestion, tool);
    }
}
