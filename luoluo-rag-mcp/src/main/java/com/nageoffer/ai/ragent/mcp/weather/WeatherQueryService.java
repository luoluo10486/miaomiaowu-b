package com.nageoffer.ai.ragent.mcp.weather;

import cn.hutool.core.util.StrUtil;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
public class WeatherQueryService {

    private static final int DEFAULT_FORECAST_DAYS = 3;

    private final WeatherApiClient weatherApiClient;

    public WeatherQueryService(WeatherApiClient weatherApiClient) {
        this.weatherApiClient = weatherApiClient;
    }

    public CallToolResult handle(CallToolRequest request, McpTransportContext transportContext) {
        try {
            Map<String, Object> args = request == null || request.arguments() == null ? Map.of() : request.arguments();
            String queryType = defaultIfBlank(stringArg(args, "queryType"), "current");
            int forecastDays = clamp(intArg(args, "days"), DEFAULT_FORECAST_DAYS, 1, 7);

            WeatherLocation location = resolveLocation(args, transportContext)
                    .orElseThrow(() -> new IllegalArgumentException("无法自动定位用户位置，请补充城市名、经纬度或 IP。"));

            WeatherSnapshot snapshot = weatherApiClient.fetchWeather(location, forecastDays);
            String content = "forecast".equalsIgnoreCase(queryType)
                    ? formatForecast(snapshot, forecastDays)
                    : formatCurrent(snapshot);
            return successResult(content);
        } catch (Exception exception) {
            return errorResult("天气查询失败: " + messageOf(exception));
        }
    }

    private Optional<WeatherLocation> resolveLocation(Map<String, Object> args, McpTransportContext transportContext) {
        Double latitude = doubleArg(args, "latitude");
        Double longitude = doubleArg(args, "longitude");
        String city = stringArg(args, "city");

        if (latitude != null || longitude != null) {
            if (latitude == null || longitude == null) {
                throw new IllegalArgumentException("经纬度需要同时提供 latitude 和 longitude。");
            }
            return Optional.of(new WeatherLocation("用户坐标", latitude, longitude));
        }

        if (StrUtil.isNotBlank(city)) {
            Optional<WeatherLocation> resolved = weatherApiClient.resolveByCity(city.trim());
            if (resolved.isPresent()) {
                return resolved;
            }
            throw new IllegalArgumentException("未找到城市「" + city.trim() + "」，请换一个更具体的城市名或直接提供经纬度。");
        }

        for (String candidate : resolveIpCandidates(args, transportContext)) {
            Optional<WeatherLocation> location = weatherApiClient.resolveByIp(candidate);
            if (location.isPresent()) {
                return location;
            }
        }

        return Optional.empty();
    }

    private List<String> resolveIpCandidates(Map<String, Object> args, McpTransportContext transportContext) {
        List<String> candidates = new ArrayList<>();
        addIpCandidate(candidates, stringArg(args, "ip"));
        addIpCandidate(candidates, transportString(transportContext, "X-Forwarded-For"));
        addIpCandidate(candidates, transportString(transportContext, "X-Real-IP"));
        addIpCandidate(candidates, transportString(transportContext, "remoteAddr"));
        return candidates;
    }

    private void addIpCandidate(List<String> candidates, String rawValue) {
        if (StrUtil.isBlank(rawValue)) {
            return;
        }
        for (String candidate : rawValue.split(",")) {
            String trimmed = candidate == null ? null : candidate.trim();
            if (StrUtil.isNotBlank(trimmed) && !candidates.contains(trimmed)) {
                candidates.add(trimmed);
            }
        }
    }

    private String formatCurrent(WeatherSnapshot snapshot) {
        ForecastResponse forecast = snapshot.forecast();
        CurrentWeather current = forecast.current();
        DailyWeather daily = forecast.daily();

        StringBuilder sb = new StringBuilder();
        sb.append("【").append(snapshot.location().label()).append(" 今日天气】\n");
        sb.append("经纬度：")
                .append(formatNumber(snapshot.location().latitude()))
                .append(", ")
                .append(formatNumber(snapshot.location().longitude()))
                .append("\n");

        if (current != null) {
            sb.append("当前：").append(weatherText(current.weatherCode()))
                    .append("，").append(formatTemperature(current.temperature2m()))
                    .append("，体感 ").append(formatTemperature(current.apparentTemperature()))
                    .append("，湿度 ").append(formatInteger(current.relativeHumidity2m())).append("%\n");
            sb.append("风：").append(directionText(current.windDirection10m()))
                    .append(" ").append(formatSpeed(current.windSpeed10m())).append("m/s");
            if (current.precipitation() != null) {
                sb.append("，降水 ").append(formatNumber(current.precipitation())).append("mm");
            }
            sb.append("\n");
        }

        if (daily != null && !isEmpty(daily.time())) {
            int index = 0;
            sb.append("今日：")
                    .append(weatherAt(daily.weatherCode(), index))
                    .append("，最高 ").append(formatTemperature(at(daily.temperature2mMax(), index)))
                    .append("，最低 ").append(formatTemperature(at(daily.temperature2mMin(), index)));
            Double precipitation = at(daily.precipitationSum(), index);
            if (precipitation != null) {
                sb.append("，降水 ").append(formatNumber(precipitation)).append("mm");
            }
            sb.append("\n");
        }

        return sb.toString().trim();
    }

    private String formatForecast(WeatherSnapshot snapshot, int forecastDays) {
        ForecastResponse forecast = snapshot.forecast();
        DailyWeather daily = forecast.daily();
        StringBuilder sb = new StringBuilder();
        sb.append("【").append(snapshot.location().label()).append(" 未来")
                .append(forecastDays).append("天预报】\n");
        if (daily == null || isEmpty(daily.time())) {
            sb.append("暂无可用的预报数据。");
            return sb.toString();
        }

        int count = Math.min(forecastDays, daily.time().size());
        for (int i = 0; i < count; i++) {
            sb.append(daily.time().get(i)).append(" | ")
                    .append(weatherAt(daily.weatherCode(), i)).append(" | ")
                    .append(formatTemperature(at(daily.temperature2mMin(), i)))
                    .append("~")
                    .append(formatTemperature(at(daily.temperature2mMax(), i)))
                    .append(" | 降水 ")
                    .append(formatNumber(at(daily.precipitationSum(), i)))
                    .append("mm");
            Double windSpeed = at(daily.windSpeed10mMax(), i);
            if (windSpeed != null) {
                sb.append(" | 最大风速 ").append(formatNumber(windSpeed)).append("m/s");
            }
            sb.append("\n");
        }
        return sb.toString().trim();
    }

    private static String weatherAt(List<Integer> weatherCodes, int index) {
        return weatherText(at(weatherCodes, index));
    }

    private static String weatherText(Integer weatherCode) {
        if (weatherCode == null) {
            return "天气未知";
        }
        return switch (weatherCode) {
            case 0 -> "晴";
            case 1 -> "大部晴朗";
            case 2 -> "局部多云";
            case 3 -> "多云";
            case 45, 48 -> "雾";
            case 51, 53, 55 -> "毛毛雨";
            case 56, 57 -> "冻雨";
            case 61, 63, 65 -> "降雨";
            case 66, 67 -> "冻雨";
            case 71, 73, 75 -> "降雪";
            case 77 -> "雪粒";
            case 80, 81, 82 -> "阵雨";
            case 85, 86 -> "阵雪";
            case 95 -> "雷暴";
            case 96, 99 -> "雷暴伴冰雹";
            default -> "天气代码 " + weatherCode;
        };
    }

    private static String directionText(Integer degrees) {
        if (degrees == null) {
            return "风向未知";
        }
        int normalized = ((degrees % 360) + 360) % 360;
        String[] directions = {"北风", "东北风", "东风", "东南风", "南风", "西南风", "西风", "西北风"};
        int index = (int) Math.round(normalized / 45.0) % directions.length;
        return directions[index];
    }

    private static <T> T at(List<T> values, int index) {
        if (values == null || index < 0 || index >= values.size()) {
            return null;
        }
        return values.get(index);
    }

    private static boolean isEmpty(List<?> values) {
        return values == null || values.isEmpty();
    }

    private static String formatTemperature(Double value) {
        return value == null ? "--℃" : String.format(Locale.ROOT, "%.1f℃", value);
    }

    private static String formatNumber(Double value) {
        return value == null ? "--" : String.format(Locale.ROOT, "%.1f", value);
    }

    private static String formatInteger(Integer value) {
        return value == null ? "--" : String.valueOf(value);
    }

    private static String formatSpeed(Double value) {
        return value == null ? "--" : String.format(Locale.ROOT, "%.1f", value);
    }

    private static String stringArg(Map<String, Object> args, String key) {
        Object value = args.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private static Double doubleArg(Map<String, Object> args, String key) {
        Object value = args.get(key);
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value == null) {
            return null;
        }
        try {
            return Double.parseDouble(String.valueOf(value).trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
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
        } catch (NumberFormatException ignored) {
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

    private static String transportString(McpTransportContext context, String key) {
        if (context == null) {
            return null;
        }
        Object value = context.get(key);
        return value == null ? null : String.valueOf(value);
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
}
