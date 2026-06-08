package com.nageoffer.ai.ragent.mcp.executor;

import com.nageoffer.ai.ragent.mcp.weather.WeatherQueryService;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class WeatherMcpExecutor {

    private static final String TOOL_ID = "weather_query";

    private final WeatherQueryService weatherQueryService;

    public WeatherMcpExecutor(WeatherQueryService weatherQueryService) {
        this.weatherQueryService = weatherQueryService;
    }

    @Bean
    public McpServerFeatures.SyncToolSpecification weatherToolSpecification() {
        return new McpServerFeatures.SyncToolSpecification(
                buildTool(),
                (McpSyncServerExchange exchange, CallToolRequest request) -> {
                    McpTransportContext transportContext = exchange == null ? McpTransportContext.EMPTY : exchange.transportContext();
                    return weatherQueryService.handle(request, transportContext);
                }
        );
    }

    private Tool buildTool() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("city", Map.of(
                "type", "string",
                "description", "城市名称，例如北京、上海、深圳。未提供时会尝试根据请求上下文自动定位。"
        ));
        properties.put("latitude", Map.of(
                "type", "number",
                "description", "纬度，和 longitude 一起使用。优先级高于 city。"
        ));
        properties.put("longitude", Map.of(
                "type", "number",
                "description", "经度，和 latitude 一起使用。优先级高于 city。"
        ));
        properties.put("ip", Map.of(
                "type", "string",
                "description", "可选的用户 IP。一般无需手动传，服务端会优先读取请求上下文中的 X-Forwarded-For / X-Real-IP。"
        ));
        properties.put("queryType", Map.of(
                "type", "string",
                "description", "查询类型：current=当前天气，forecast=未来预报。",
                "enum", List.of("current", "forecast"),
                "default", "current"
        ));
        properties.put("days", Map.of(
                "type", "integer",
                "description", "预报天数，仅 forecast 模式有效，默认 3 天，最大 7 天。",
                "default", 3
        ));

        return Tool.builder()
                .name(TOOL_ID)
                .description("查询用户当地今天的真实天气，支持城市、经纬度和请求上下文自动定位。")
                .inputSchema(new io.modelcontextprotocol.spec.McpSchema.JsonSchema(
                        "object",
                        properties,
                        List.of(),
                        null,
                        null,
                        null
                ))
                .build();
    }
}
