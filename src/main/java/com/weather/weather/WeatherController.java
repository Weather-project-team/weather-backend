package com.weather.weather;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/weather")
@CrossOrigin(origins = "http://localhost:5173") // React 프론트엔드 연결
public class WeatherController {

    @Autowired
    private WeatherService weatherService;

    // ✅ 1️⃣ 특정 도시의 날씨 정보를 가져오는 API
    @GetMapping
    public ResponseEntity<Map<String, Object>> getWeatherData(@RequestParam(required = false) String city) {
        if (city == null || city.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "도시명을 입력하세요."));
        }

        try {
            Map<String, String> formattedWeatherData = weatherService.getFormattedWeatherData(city);
            return ResponseEntity.ok(Map.of("city", city, "weather", formattedWeatherData));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "날씨 데이터를 가져오지 못했습니다."));
        }
    }

    // ✅ 2️⃣ 가장 가까운 도시 찾기 (위도, 경도 기반)
    @GetMapping("/nearest-city")
    public ResponseEntity<Map<String, String>> getNearestCity(@RequestParam double lat, @RequestParam double lon) {
        try {
            String city = weatherService.findClosestCity(lat, lon);
            return ResponseEntity.ok(Map.of("city", city));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "가장 가까운 도시를 찾을 수 없습니다."));
        }
    }

    // ✅ 3️⃣ 저장된 도시 목록 반환 (검색 및 자동완성용)
    @GetMapping("/cities")
    public ResponseEntity<List<String>> getCityList(@RequestParam(required = false) String query) {
        List<String> cities = weatherService.getCityNames();

        // 검색어(query)가 있을 경우 필터링
        if (query != null && !query.isBlank()) {
            cities = cities.stream()
                    .filter(city -> city.toLowerCase().contains(query.toLowerCase()))
                    .collect(Collectors.toList());
        }

        return ResponseEntity.ok(cities);
    }
}
