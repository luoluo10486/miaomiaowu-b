package com.personalblog.ragbackend.rag.core.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.personalblog.ragbackend.infra.chat.LLMService;
import com.personalblog.ragbackend.infra.convention.ChatRequest;
import com.personalblog.ragbackend.rag.core.prompt.PromptTemplateLoader;
import io.modelcontextprotocol.spec.McpSchema.JsonSchema;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LLMMcpParameterExtractorTest {

    @Mock
    private LLMService llmService;

    @Mock
    private PromptTemplateLoader promptTemplateLoader;

    private LLMMcpParameterExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new LLMMcpParameterExtractor(llmService, promptTemplateLoader, new ObjectMapper());
        when(promptTemplateLoader.load(anyString())).thenReturn("system-prompt");
        when(promptTemplateLoader.render(anyString(), anyMap())).thenReturn("user-prompt");
    }

    @Test
    void shouldFallbackToHeuristicWeatherCityWhenModelReturnsInvalidJson() {
        when(llmService.chat(org.mockito.ArgumentMatchers.any(ChatRequest.class))).thenReturn("not json");

        Tool tool = weatherTool();
        Map<String, Object> params = extractor.extractParameters("广州今天的天气怎么样？", tool);

        assertThat(params).containsEntry("city", "广州");
        assertThat(params).containsEntry("queryType", "current");
        assertThat(params).containsEntry("days", 3);
    }

    @Test
    void shouldNormalizeNoisyWeatherCityReturnedByModel() {
        when(llmService.chat(org.mockito.ArgumentMatchers.any(ChatRequest.class)))
                .thenReturn("{\"city\":\"广州今天的天气怎么样？\",\"queryType\":\"current\"}");

        Tool tool = weatherTool();
        Map<String, Object> params = extractor.extractParameters("广州今天的天气怎么样？", tool);

        assertThat(params).containsEntry("city", "广州");
        assertThat(params).containsEntry("queryType", "current");
        assertThat(params).containsEntry("days", 3);
    }

    @Test
    void shouldReturnForecastDefaultsForForecastQuestion() {
        when(llmService.chat(org.mockito.ArgumentMatchers.any(ChatRequest.class))).thenReturn("");

        Tool tool = weatherTool();
        Map<String, Object> params = extractor.extractParameters("广州未来3天的天气预报", tool);

        assertThat(params).containsEntry("city", "广州");
        assertThat(params).containsEntry("queryType", "forecast");
        assertThat(params).containsEntry("days", 3);
    }

    private Tool weatherTool() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("city", Map.of("type", "string"));
        properties.put("queryType", Map.of(
                "type", "string",
                "default", "current"
        ));
        properties.put("days", Map.of(
                "type", "integer",
                "default", 3
        ));

        return Tool.builder()
                .name("weather_query")
                .description("weather query")
                .inputSchema(new JsonSchema("object", properties, List.of(), null, null, null))
                .build();
    }
}
