package com.personalblog.ragbackend.rag.core.mcp;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 默认MCPTool注册表
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultMcpToolRegistry implements McpToolRegistry {

    private final Map<String, McpToolExecutor> executorMap = new HashMap<>();
    private final List<McpToolExecutor> autoDiscoveredExecutors;

    @PostConstruct
    public void init() {
        if (CollUtil.isEmpty(autoDiscoveredExecutors)) {
            log.info("MCP tool registry skipped, no executors discovered");
            return;
        }
        for (McpToolExecutor executor : autoDiscoveredExecutors) {
            register(executor);
        }
        log.info("MCP tool registry initialized, registered {} tools", autoDiscoveredExecutors.size());
    }

    @Override
    public void register(McpToolExecutor executor) {
        if (executor == null || executor.getToolDefinition() == null) {
            log.warn("Ignore empty MCP executor");
            return;
        }

        String toolId = executor.getToolId();
        if (StrUtil.isBlank(toolId)) {
            log.warn("Ignore MCP executor with blank toolId");
            return;
        }

        McpToolExecutor existing = executorMap.put(toolId, executor);
        if (existing != null) {
            log.warn("MCP tool already exists, replaced toolId={}", toolId);
        } else {
            log.info("MCP tool registered, toolId={}", toolId);
        }
    }

    @Override
    public void unregister(String toolId) {
        McpToolExecutor removed = executorMap.remove(toolId);
        if (removed != null) {
            log.info("MCP tool unregistered, toolId={}", toolId);
        }
    }

    @Override
    public Optional<McpToolExecutor> getExecutor(String toolId) {
        return Optional.ofNullable(executorMap.get(toolId));
    }

    @Override
    public List<Tool> listAllTools() {
        return executorMap.values().stream()
                .map(McpToolExecutor::getToolDefinition)
                .toList();
    }

    @Override
    public List<McpToolExecutor> listAllExecutors() {
        return new ArrayList<>(executorMap.values());
    }

    @Override
    public boolean contains(String toolId) {
        return executorMap.containsKey(toolId);
    }

    @Override
    public int size() {
        return executorMap.size();
    }
}
