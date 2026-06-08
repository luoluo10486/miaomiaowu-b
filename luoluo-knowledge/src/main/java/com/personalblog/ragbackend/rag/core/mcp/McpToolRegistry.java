package com.personalblog.ragbackend.rag.core.mcp;

import io.modelcontextprotocol.spec.McpSchema.Tool;

import java.util.List;
import java.util.Optional;

/**
 * MCPTool注册表
 */
public interface McpToolRegistry {
    void register(McpToolExecutor executor);

    void unregister(String toolId);

    Optional<McpToolExecutor> getExecutor(String toolId);

    List<Tool> listAllTools();

    List<McpToolExecutor> listAllExecutors();

    boolean contains(String toolId);

    int size();
}
