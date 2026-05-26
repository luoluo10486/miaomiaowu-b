package com.nageoffer.ai.ragent.mcp.executor;

import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.JsonSchema;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Component
public class WeatherMcpExecutor {

    private static final String TOOL_ID = "weather_query";
    private static final List<String> WEATHER_TYPES = List.of("晴", "多云", "阴", "小雨", "阵雨", "雷阵雨", "大雨");
    private static final List<String> WIND_DIRECTIONS = List.of("东风", "南风", "西风", "北风", "东南风", "东北风", "西南风", "西北风");

    @Bean
    public McpServerFeatures.SyncToolSpecification weatherToolSpecification() {
        return new McpServerFeatures.SyncToolSpecification(buildTool(), (exchange, request) -> handleCall(request));
    }

    private Tool buildTool() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("city", Map.of("type", "string", "description", "城市名称，例如北京、上海、广州、深圳"));
        properties.put("queryType", Map.of(
                "type", "string",
                "description", "查询类型：current(当前天气)、forecast(未来预报)",
                "enum", List.of("current", "forecast"),
                "default", "current"
        ));
        properties.put("days", Map.of(
                "type", "integer",
                "description", "预报天数，仅 forecast 模式有效，默认 3 天，最大 7 天",
                "default", 3
        ));

        JsonSchema inputSchema = new JsonSchema("object", properties, List.of("city"), null, null, null);
        return Tool.builder()
                .name(TOOL_ID)
                .description("查询城市天气信息，支持查看当前实时天气和未来多天天气预报")
                .inputSchema(inputSchema)
                .build();
    }

    private CallToolResult handleCall(CallToolRequest request) {
        try {
            Map<String, Object> args = request.arguments() == null ? Map.of() : request.arguments();
            String city = stringArg(args, "city");
            String queryType = defaultIfBlank(stringArg(args, "queryType"), "current");
            int days = clamp(intArg(args, "days"), 3, 1, 7);

            if (city == null || city.isBlank()) {
                return errorResult("请提供城市名称");
            }

            String result = "forecast".equals(queryType)
                    ? buildForecastResult(city, days)
                    : buildCurrentResult(city);
            return successResult(result);
        } catch (Exception exception) {
            return errorResult("查询失败: " + messageOf(exception));
        }
    }

    private String buildCurrentResult(String city) {
        LocalDate today = LocalDate.now();
        WeatherData weather = generate(city, today);
        StringBuilder sb = new StringBuilder();
        sb.append("【").append(city).append(" 今日天气】\n");
        sb.append("日期: ").append(today.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))).append("\n");
        sb.append("天气: ").append(weather.weatherType).append("\n");
        sb.append("当前温度: ").append(weather.currentTemp).append("℃\n");
        sb.append("最高温度: ").append(weather.highTemp).append("℃\n");
        sb.append("最低温度: ").append(weather.lowTemp).append("℃\n");
        sb.append("湿度: ").append(weather.humidity).append("%\n");
        sb.append("风向: ").append(weather.windDirection).append("\n");
        sb.append("风力: ").append(weather.windLevel).append("\n");
        sb.append("空气质量: ").append(weather.airQuality).append("\n");
        return sb.toString().trim();
    }

    private String buildForecastResult(String city, int days) {
        LocalDate today = LocalDate.now();
        StringBuilder sb = new StringBuilder();
        sb.append("【").append(city).append(" 未来").append(days).append("天天气预报】\n\n");
        for (int i = 0; i < days; i++) {
            LocalDate date = today.plusDays(i);
            WeatherData weather = generate(city, date);
            sb.append(date.format(DateTimeFormatter.ofPattern("MM-dd")))
                    .append(" | ")
                    .append(weather.weatherType)
                    .append(" | ")
                    .append(weather.lowTemp)
                    .append("℃~")
                    .append(weather.highTemp)
                    .append("℃ | ")
                    .append(weather.windDirection)
                    .append(" ")
                    .append(weather.windLevel)
                    .append("\n");
        }
        return sb.toString().trim();
    }

    private WeatherData generate(String city, LocalDate date) {
        Random random = new Random((city + date).hashCode());
        int month = date.getMonthValue();
        int seasonalBase = switch (month) {
            case 3, 4, 5 -> 18;
            case 6, 7, 8 -> 30;
            case 9, 10, 11 -> 22;
            default -> 6;
        };
        int highTemp = seasonalBase + random.nextInt(6);
        int lowTemp = seasonalBase - 6 - random.nextInt(4);
        int currentTemp = lowTemp + random.nextInt(Math.max(1, highTemp - lowTemp + 1));
        String weatherType = WEATHER_TYPES.get(random.nextInt(WEATHER_TYPES.size()));
        String windDirection = WIND_DIRECTIONS.get(random.nextInt(WIND_DIRECTIONS.size()));
        String windLevel = (1 + random.nextInt(5)) + "-" + (2 + random.nextInt(4)) + "级";
        int humidity = 35 + random.nextInt(55);
        String airQuality = switch (random.nextInt(4)) {
            case 0 -> "优";
            case 1 -> "良";
            case 2 -> "轻度污染";
            default -> "中度污染";
        };
        return new WeatherData(weatherType, currentTemp, highTemp, lowTemp, humidity, windDirection, windLevel, airQuality);
    }

    private static String stringArg(Map<String, Object> args, String key) {
        Object value = args.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private static Integer intArg(Map<String, Object> args, String key) {
        Object value = args.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return null;
        }
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static int clamp(Integer value, int defaultValue, int min, int max) {
        int actual = value == null ? defaultValue : value;
        return Math.max(min, Math.min(max, actual));
    }

    private static String defaultIfBlank(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private static CallToolResult successResult(String text) {
        return CallToolResult.builder()
                .content(List.of(new TextContent(text == null ? "" : text)))
                .isError(false)
                .build();
    }

    private static CallToolResult errorResult(String message) {
        return CallToolResult.builder()
                .content(List.of(new TextContent(message == null ? "" : message)))
                .isError(true)
                .build();
    }

    private static String messageOf(Exception exception) {
        return exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
    }

    private record WeatherData(String weatherType,
                               int currentTemp,
                               int highTemp,
                               int lowTemp,
                               int humidity,
                               String windDirection,
                               String windLevel,
                               String airQuality) {
    }
}
