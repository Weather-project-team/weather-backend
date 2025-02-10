package com.weather.weather;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/weather")
@CrossOrigin(origins = "http://localhost:5173")
public class WeatherController {

    @Autowired
    private WeatherService weatherService;

    @GetMapping
    public Map<String, Object> getWeatherData(@RequestParam(required = false) String city) {
        if (city == null || city.isEmpty()) {
            return Map.of("error", "도시명을 입력하세요.");
        }

        try {
            Map<String, String> formattedWeatherData = weatherService.getFormattedWeatherData(city);
            return Map.of("city", city, "weather", formattedWeatherData);
        } catch (Exception e) {
            return Map.of("error", "날씨 데이터를 가져오지 못했습니다.");
        }
    }
}
