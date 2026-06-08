package com.personalblog.ragbackend.rag.core.mcp;

import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

/**
 * MCP客户端Tool执行器
 */
@Slf4j
public class McpClientToolExecutor implements McpToolExecutor {

    private final McpSyncClient mcpClient;
    private final Tool toolDefinition;

    public McpClientToolExecutor(McpSyncClient mcpClient, Tool toolDefinition, String serverUrl) {
        this.mcpClient = mcpClient;
        this.toolDefinition = toolDefinition;
    }

    @Override
    public Tool getToolDefinition() {
        return toolDefinition;
    }

    @Override
    public CallToolResult execute(Map<String, Object> parameters) {
        long startMs = System.currentTimeMillis();
        try {
            Map<String, Object> args = parameters == null ? Map.of() : parameters;
            CallToolResult result = mcpClient.callTool(new CallToolRequest(toolDefinition.name(), args));
            log.info("remote MCP tool call complete, toolId={}, params={}, contentSize={}, elapsed={}ms",
                    toolDefinition.name(), args, result.content() == null ? 0 : result.content().size(), System.currentTimeMillis() - startMs);
            return result;
        } catch (Exception exception) {
            String reason = exception.getMessage() != null ? exception.getMessage() : exception.getClass().getSimpleName();
            log.warn("remote MCP tool call failed, toolId={}, reason={}", toolDefinition.name(), reason);
            return CallToolResult.builder()
                    .content(List.of(new TextContent("remote MCP tool call failed: " + reason)))
                    .isError(true)
                    .build();
        }
    }
}
