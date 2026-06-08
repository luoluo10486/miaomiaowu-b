package com.nageoffer.ai.ragent.mcp.weather;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Component
public class OpenMeteoWeatherApiClient implements WeatherApiClient {

    private static final String GEOCODING_ENDPOINT = "https://geocoding-api.open-meteo.com/v1/search";
    private static final String FORECAST_ENDPOINT = "https://api.open-meteo.com/v1/forecast";
    private static final String IP_LOOKUP_ENDPOINT = "https://ipwho.is/";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public OpenMeteoWeatherApiClient(RestTemplateBuilder restTemplateBuilder, ObjectMapper objectMapper) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<WeatherLocation> resolveByCity(String city) {
        if (StrUtil.isBlank(city)) {
            return Optional.empty();
        }
        String url = UriComponentsBuilder.fromHttpUrl(GEOCODING_ENDPOINT)
                .queryParam("name", city.trim())
                .queryParam("count", 1)
                .queryParam("language", "zh")
                .queryParam("format", "json")
                .build(true)
                .toUriString();
        GeocodingResponse response = getForObject(url, GeocodingResponse.class);
        if (response == null || CollUtil.isEmpty(response.results())) {
            return Optional.empty();
        }
        GeocodingResult result = response.results().get(0);
        if (result == null || result.latitude() == null || result.longitude() == null) {
            return Optional.empty();
        }
        return Optional.of(new WeatherLocation(buildLabel(result.name(), result.admin1(), result.country()),
                result.latitude(), result.longitude()));
    }

    @Override
    public Optional<WeatherLocation> resolveByIp(String ip) {
        String normalized = normalizeIp(ip);
        if (StrUtil.isBlank(normalized)) {
            return Optional.empty();
        }
        String url = IP_LOOKUP_ENDPOINT + URLEncoder.encode(normalized, StandardCharsets.UTF_8);
        IpLookupResponse response = getForObject(url, IpLookupResponse.class);
        if (response == null || !Boolean.TRUE.equals(response.success())
                || response.latitude() == null || response.longitude() == null) {
            return Optional.empty();
        }
        return Optional.of(new WeatherLocation(buildLabel(response.city(), response.region(), response.country()),
                response.latitude(), response.longitude()));
    }

    @Override
    public WeatherSnapshot fetchWeather(WeatherLocation location, int forecastDays) {
        String url = UriComponentsBuilder.fromHttpUrl(FORECAST_ENDPOINT)
                .queryParam("latitude", location.latitude())
                .queryParam("longitude", location.longitude())
                .queryParam("current", "temperature_2m,apparent_temperature,relative_humidity_2m,weather_code,wind_speed_10m,wind_direction_10m,precipitation")
                .queryParam("daily", "weather_code,temperature_2m_max,temperature_2m_min,precipitation_sum,wind_speed_10m_max")
                .queryParam("timezone", "auto")
                .queryParam("forecast_days", forecastDays)
                .build(true)
                .toUriString();
        ForecastResponse response = getForObject(url, ForecastResponse.class);
        if (response == null || response.current() == null) {
            throw new IllegalStateException("天气服务暂时不可用，请稍后再试");
        }
        return new WeatherSnapshot(location, response);
    }

    private <T> T getForObject(String url, Class<T> type) {
        try {
            String body = restTemplate.getForObject(URI.create(url), String.class);
            if (StrUtil.isBlank(body)) {
                return null;
            }
            return objectMapper.readValue(body, type);
        } catch (RestClientException exception) {
            throw new IllegalStateException("天气服务请求失败: " + messageOf(exception), exception);
        } catch (Exception exception) {
            throw new IllegalStateException("天气服务响应解析失败: " + messageOf(exception), exception);
        }
    }

    private static String buildLabel(String first, String second, String third) {
        List<String> parts = Stream.of(first, second, third)
                .filter(StrUtil::isNotBlank)
                .map(String::trim)
                .distinct()
                .toList();
        if (parts.isEmpty()) {
            return "未知位置";
        }
        return String.join(", ", parts);
    }

    private static String normalizeIp(String ip) {
        if (StrUtil.isBlank(ip)) {
            return null;
        }
        String candidate = ip.trim();
        if (candidate.contains(",")) {
            candidate = candidate.substring(0, candidate.indexOf(','));
        }
        candidate = candidate.trim();
        if (candidate.startsWith("\"") && candidate.endsWith("\"") && candidate.length() > 1) {
            candidate = candidate.substring(1, candidate.length() - 1);
        }
        if (candidate.startsWith("[") && candidate.contains("]")) {
            candidate = candidate.substring(1, candidate.indexOf(']'));
        } else if (candidate.indexOf('.') >= 0 && candidate.indexOf(':') > 0 && candidate.chars().filter(ch -> ch == ':').count() == 1) {
            candidate = candidate.substring(0, candidate.lastIndexOf(':'));
        }
        return candidate;
    }

    private static String messageOf(Exception exception) {
        return exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
    }
}
