package com.nageoffer.ai.ragent.mcp.weather;

import java.util.Optional;

public interface WeatherApiClient {
    Optional<WeatherLocation> resolveByCity(String city);

    Optional<WeatherLocation> resolveByIp(String ip);

    WeatherSnapshot fetchWeather(WeatherLocation location, int forecastDays);
}
