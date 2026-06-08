package com.nageoffer.ai.ragent.mcp.weather;

import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class WeatherQueryServiceTest {

    @Test
    void shouldPreferCoordinatesOverCityAndIp() {
        TrackingClient client = new TrackingClient();
        WeatherQueryService service = new WeatherQueryService(client);

        CallToolResult result = service.handle(
                new CallToolRequest("weather_query", Map.of(
                        "city", "北京",
                        "latitude", 39.9,
                        "longitude", 116.4,
                        "ip", "1.1.1.1"
                )),
                McpTransportContext.create(Map.of("X-Forwarded-For", "2.2.2.2"))
        );

        assertThat(Boolean.TRUE.equals(result.isError())).isFalse();
        assertThat(client.cityLookups).isEqualTo(0);
        assertThat(client.ipLookups).isEqualTo(0);
        assertThat(client.lastLocation).isNotNull();
        assertThat(client.lastLocation.label()).isEqualTo("用户坐标");
        assertThat(firstText(result)).contains("用户坐标");
    }

    @Test
    void shouldResolveByCityWhenCoordinatesMissing() {
        TrackingClient client = new TrackingClient();
        WeatherQueryService service = new WeatherQueryService(client);

        CallToolResult result = service.handle(
                new CallToolRequest("weather_query", Map.of("city", "北京")),
                McpTransportContext.EMPTY
        );

        assertThat(Boolean.TRUE.equals(result.isError())).isFalse();
        assertThat(client.cityLookups).isEqualTo(1);
        assertThat(client.ipLookups).isEqualTo(0);
        assertThat(firstText(result)).contains("北京");
    }

    @Test
    void shouldResolveByForwardedIpWhenCityMissing() {
        TrackingClient client = new TrackingClient();
        WeatherQueryService service = new WeatherQueryService(client);

        CallToolResult result = service.handle(
                new CallToolRequest("weather_query", Map.of()),
                McpTransportContext.create(Map.of("X-Forwarded-For", "203.0.113.8, 10.0.0.1"))
        );

        assertThat(Boolean.TRUE.equals(result.isError())).isFalse();
        assertThat(client.ipLookups).isEqualTo(1);
        assertThat(client.lastIp).isEqualTo("203.0.113.8");
        assertThat(firstText(result)).contains("上海");
    }

    @Test
    void shouldReturnFriendlyErrorWhenLocationCannotBeResolved() {
        TrackingClient client = new TrackingClient();
        WeatherQueryService service = new WeatherQueryService(client);

        CallToolResult result = service.handle(
                new CallToolRequest("weather_query", Map.of()),
                McpTransportContext.EMPTY
        );

        assertThat(Boolean.TRUE.equals(result.isError())).isTrue();
        assertThat(firstText(result)).contains("无法自动定位用户位置");
    }

    private static String firstText(CallToolResult result) {
        return result.content().stream()
                .filter(TextContent.class::isInstance)
                .map(TextContent.class::cast)
                .map(TextContent::text)
                .findFirst()
                .orElse("");
    }

    private static final class TrackingClient implements WeatherApiClient {
        private int cityLookups;
        private int ipLookups;
        private String lastIp;
        private WeatherLocation lastLocation;

        @Override
        public Optional<WeatherLocation> resolveByCity(String city) {
            cityLookups++;
            if ("北京".equals(city)) {
                return Optional.of(new WeatherLocation("北京, 中国", 39.9, 116.4));
            }
            return Optional.empty();
        }

        @Override
        public Optional<WeatherLocation> resolveByIp(String ip) {
            ipLookups++;
            lastIp = ip;
            if ("203.0.113.8".equals(ip)) {
                return Optional.of(new WeatherLocation("上海, 中国", 31.2, 121.5));
            }
            return Optional.empty();
        }

        @Override
        public WeatherSnapshot fetchWeather(WeatherLocation location, int forecastDays) {
            lastLocation = location;
            return new WeatherSnapshot(location, sampleForecast());
        }

        private ForecastResponse sampleForecast() {
            return new ForecastResponse(
                    39.9,
                    116.4,
                    "Asia/Shanghai",
                    new CurrentWeather("2026-06-08T10:00", 25.4, 26.0, 52, 0, 3.4, 90, 0.0),
                    new DailyWeather(
                            List.of("2026-06-08", "2026-06-09"),
                            List.of(0, 61),
                            List.of(22.0, 23.0),
                            List.of(30.0, 28.0),
                            List.of(0.0, 1.2),
                            List.of(4.1, 5.3)
                    )
            );
        }
    }
}
