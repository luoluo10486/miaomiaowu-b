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
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * LLMMCPParameter提取器类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LLMMcpParameterExtractor implements McpParameterExtractor {
    private static final String WEATHER_TOOL_ID = "weather_query";
    private static final Pattern WEATHER_QUERY_HINT_PATTERN = Pattern.compile(
            "(今天|明天|后天|未来|这周|本周|下周|现在|今晚|早上|下午|晚上|凌晨|天气|预报|气温|温度|下雨|下雪|湿度|风速|风向|怎么样|如何|好吗|么)"
    );
    private static final Pattern WEATHER_CITY_PREFIX_PATTERN = Pattern.compile("^\\s*(请问|帮我|麻烦|想知道|告诉我|查一下|查询一下|查下|看看|帮忙)?\\s*(我在|我这里|我这边|当地|本地|当前位置|我所在|所在|这里|附近)?\\s*");
    private static final Pattern WEATHER_CITY_SUFFIX_PATTERN = Pattern.compile("[\\s\\p{Punct}·、，。；;：:!?？！（）()【】\\[\\]\"'`~]+$");

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
            extracted.putAll(buildWeatherFallbackParameters(userQuestion, tool, extracted));
            fillDefaults(extracted, tool);
            return extracted;
        } catch (Exception exception) {
            log.warn("MCP parameter extraction failed, toolId={}", tool.name(), exception);
            Map<String, Object> fallback = buildWeatherFallbackParameters(userQuestion, tool, Map.of());
            return fillDefaults(fallback, tool);
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

    private Map<String, Object> buildWeatherFallbackParameters(String userQuestion, Tool tool, Map<String, Object> extracted) {
        if (!isWeatherTool(tool)) {
            return Collections.emptyMap();
        }

        Map<String, Object> fallback = new HashMap<>();
        if (containsKeyWithValue(extracted, "city")) {
            String normalizedCity = extractWeatherCityCandidate(String.valueOf(extracted.get("city")));
            if (StrUtil.isNotBlank(normalizedCity)) {
                String currentCity = String.valueOf(extracted.get("city")).trim();
                if (!normalizedCity.equals(currentCity)) {
                    fallback.put("city", normalizedCity);
                }
            }
        } else if (extracted == null || !hasAnyLocationHint(extracted)) {
            String city = extractWeatherCityCandidate(userQuestion);
            if (StrUtil.isNotBlank(city)) {
                fallback.put("city", city);
            }
        }

        if (!containsKeyWithValue(extracted, "queryType")) {
            String queryType = inferWeatherQueryType(userQuestion);
            if (StrUtil.isNotBlank(queryType)) {
                fallback.put("queryType", queryType);
            }
        }

        if (!containsKeyWithValue(extracted, "days")) {
            Integer days = inferForecastDays(userQuestion);
            if (days != null) {
                fallback.put("days", days);
            }
        }

        return fallback;
    }

    private boolean isWeatherTool(Tool tool) {
        return tool != null && WEATHER_TOOL_ID.equals(tool.name());
    }

    private boolean hasAnyLocationHint(Map<String, Object> params) {
        return containsKeyWithValue(params, "city")
                || containsKeyWithValue(params, "latitude")
                || containsKeyWithValue(params, "longitude")
                || containsKeyWithValue(params, "ip");
    }

    private boolean containsKeyWithValue(Map<String, Object> params, String key) {
        return params != null && params.containsKey(key) && params.get(key) != null && StrUtil.isNotBlank(String.valueOf(params.get(key)).trim());
    }

    private String inferWeatherQueryType(String userQuestion) {
        if (StrUtil.isBlank(userQuestion)) {
            return "";
        }
        String normalized = userQuestion.replace(" ", "");
        if (normalized.contains("未来") || normalized.contains("预报") || normalized.contains("几天")) {
            return "forecast";
        }
        return "current";
    }

    private Integer inferForecastDays(String userQuestion) {
        if (StrUtil.isBlank(userQuestion)) {
            return null;
        }
        Matcher matcher = Pattern.compile("(?:未来|接下来|之后|近)?(\\d{1,2})\\s*天").matcher(userQuestion);
        if (matcher.find()) {
            try {
                int days = Integer.parseInt(matcher.group(1));
                return days >= 1 && days <= 7 ? days : null;
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        if (userQuestion.contains("三天") || userQuestion.contains("3天")) {
            return 3;
        }
        if (userQuestion.contains("五天") || userQuestion.contains("5天")) {
            return 5;
        }
        return null;
    }

    private String extractWeatherCityCandidate(String userQuestion) {
        if (StrUtil.isBlank(userQuestion)) {
            return "";
        }

        String normalized = normalizeQuestion(userQuestion);
        if (StrUtil.isBlank(normalized)) {
            return "";
        }

        int cutoff = findWeatherCutoff(normalized);
        String prefix = cutoff > 0 ? normalized.substring(0, cutoff) : normalized;
        prefix = WEATHER_CITY_PREFIX_PATTERN.matcher(prefix).replaceFirst("");
        prefix = prefix.replaceAll("^[的在于从到去是和呀啊哦呢嘛吧]+", "");
        prefix = prefix.replaceAll("[的在于从到去是和呀啊哦呢嘛吧]+$", "");
        prefix = WEATHER_CITY_SUFFIX_PATTERN.matcher(prefix).replaceAll("");
        return prefix.trim();
    }

    private String normalizeQuestion(String question) {
        return question
                .replace("\r", "")
                .replace("\n", "")
                .replace("\t", "")
                .replace(" ", "")
                .replace("　", "")
                .trim();
    }

    private int findWeatherCutoff(String text) {
        if (StrUtil.isBlank(text)) {
            return -1;
        }
        int cutoff = -1;
        Matcher matcher = WEATHER_QUERY_HINT_PATTERN.matcher(text);
        while (matcher.find()) {
            int start = matcher.start();
            if (cutoff < 0 || start < cutoff) {
                cutoff = start;
            }
        }
        return cutoff;
    }
}
