package com.personalblog.ragbackend.rag.core.mcp;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.personalblog.ragbackend.infra.chat.LLMService;
import com.personalblog.ragbackend.infra.convention.ChatMessage;
import com.personalblog.ragbackend.infra.convention.ChatRequest;
import com.personalblog.ragbackend.infra.util.LLMResponseCleaner;
import com.personalblog.ragbackend.rag.constant.RAGConstant;
import com.personalblog.ragbackend.rag.core.prompt.PromptTemplateLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class LLMMcpParameterExtractor implements McpParameterExtractor {

    private final LLMService llmService;
    private final PromptTemplateLoader promptTemplateLoader;
    private final ObjectMapper objectMapper;

    @Override
    public Map<String, Object> extractParameters(String userQuestion, Tool tool) {
        return extractParameters(userQuestion, tool, null);
    }

    @Override
    public Map<String, Object> extractParameters(String userQuestion, Tool tool, String customPromptTemplate) {
        if (tool == null || tool.inputSchema() == null || CollUtil.isEmpty(tool.inputSchema().properties())) {
            return Collections.emptyMap();
        }

        List<ChatMessage> messages = List.of(
                ChatMessage.system(StrUtil.blankToDefault(
                        customPromptTemplate,
                        promptTemplateLoader.load(RAGConstant.MCP_PARAMETER_EXTRACT_PROMPT_PATH)
                )),
                ChatMessage.user(buildUserPrompt(userQuestion, tool))
        );

        try {
            String raw = llmService.chat(ChatRequest.builder()
                    .messages(messages)
                    .temperature(0.1D)
                    .topP(0.3D)
                    .thinking(false)
                    .build());
            Map<String, Object> extracted = parseJsonResponse(raw, tool);
            fillDefaults(extracted, tool);
            return extracted;
        } catch (Exception exception) {
            log.warn("MCP parameter extraction failed, toolId={}", tool.name(), exception);
            return fillDefaults(new HashMap<>(), tool);
        }
    }

    private String buildUserPrompt(String userQuestion, Tool tool) {
        return promptTemplateLoader.render(RAGConstant.MCP_PARAMETER_EXTRACT_USER_PROMPT_PATH, Map.of(
                "tool_definition", buildToolDefinition(tool),
                "user_question", StrUtil.blankToDefault(userQuestion, "")
        ));
    }

    private Map<String, Object> parseJsonResponse(String raw, Tool tool) throws Exception {
        if (StrUtil.isBlank(raw)) {
            return new HashMap<>();
        }
        String cleaned = LLMResponseCleaner.stripMarkdownCodeFence(raw);
        Map<String, Object> parsed = objectMapper.readValue(cleaned, new TypeReference<LinkedHashMap<String, Object>>() {
        });
        Map<String, Object> result = new HashMap<>();
        for (String key : tool.inputSchema().properties().keySet()) {
            if (parsed.containsKey(key) && parsed.get(key) != null) {
                result.put(key, parsed.get(key));
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> fillDefaults(Map<String, Object> params, Tool tool) {
        Map<String, Object> targetParams = params == null ? new HashMap<>() : params;
        tool.inputSchema().properties().forEach((name, def) -> {
            if (targetParams.containsKey(name) || !(def instanceof Map<?, ?> defMap)) {
                return;
            }
            Object defaultValue = defMap.get("default");
            if (defaultValue != null) {
                targetParams.put(name, defaultValue);
            }
        });
        return targetParams;
    }

    private String buildToolDefinition(Tool tool) {
        StringBuilder sb = new StringBuilder();
        sb.append("toolId: ").append(tool.name()).append('\n');
        sb.append("description: ").append(StrUtil.blankToDefault(tool.description(), "")).append('\n');
        sb.append("parameters: ").append(tool.inputSchema() == null ? Collections.emptyMap() : tool.inputSchema().properties());
        return sb.toString();
    }
}
