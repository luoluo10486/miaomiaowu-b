package com.nageoffer.ai.ragent.mcp.weather;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

record ForecastResponse(
        @JsonProperty("latitude") Double latitude,
        @JsonProperty("longitude") Double longitude,
        @JsonProperty("timezone") String timezone,
        @JsonProperty("current") CurrentWeather current,
        @JsonProperty("daily") DailyWeather daily
) {
}

record CurrentWeather(
        @JsonProperty("time") String time,
        @JsonProperty("temperature_2m") Double temperature2m,
        @JsonProperty("apparent_temperature") Double apparentTemperature,
        @JsonProperty("relative_humidity_2m") Integer relativeHumidity2m,
        @JsonProperty("weather_code") Integer weatherCode,
        @JsonProperty("wind_speed_10m") Double windSpeed10m,
        @JsonProperty("wind_direction_10m") Integer windDirection10m,
        @JsonProperty("precipitation") Double precipitation
) {
}

record DailyWeather(
        @JsonProperty("time") List<String> time,
        @JsonProperty("weather_code") List<Integer> weatherCode,
        @JsonProperty("temperature_2m_max") List<Double> temperature2mMax,
        @JsonProperty("temperature_2m_min") List<Double> temperature2mMin,
        @JsonProperty("precipitation_sum") List<Double> precipitationSum,
        @JsonProperty("wind_speed_10m_max") List<Double> windSpeed10mMax
) {
}

record GeocodingResponse(@JsonProperty("results") List<GeocodingResult> results) {
}

record GeocodingResult(
        @JsonProperty("name") String name,
        @JsonProperty("admin1") String admin1,
        @JsonProperty("country") String country,
        @JsonProperty("latitude") Double latitude,
        @JsonProperty("longitude") Double longitude
) {
}

record IpLookupResponse(
        @JsonProperty("success") Boolean success,
        @JsonProperty("city") String city,
        @JsonProperty("region") String region,
        @JsonProperty("country") String country,
        @JsonProperty("latitude") Double latitude,
        @JsonProperty("longitude") Double longitude
) {
}
