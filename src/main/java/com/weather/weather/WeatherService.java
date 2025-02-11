package com.weather.weather;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class WeatherService {

    private static final String API_URL = "http://apis.data.go.kr/1360000/VilageFcstInfoService_2.0/getUltraSrtNcst";
    private static final String SERVICE_KEY = "hNQQGlEEAxRJBkdL1VFgEmEDkpw5QWrK0cK3BJ0lx0mHufYq3ruuhwSS4uosgUFQDKNOOMB2fWW0iiQWJb76GA==";

    private final RestTemplate restTemplate;
    private Map<String, Integer[]> cityCoordinates;

    public WeatherService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        loadCityCoordinates();
    }

    private void loadCityCoordinates() {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            cityCoordinates = objectMapper.readValue(
                    new ClassPathResource("city-coordinates.json").getInputStream(),
                    new TypeReference<Map<String, Integer[]>>() {}
            );
        } catch (IOException e) {
            throw new RuntimeException("🚨 도시 좌표 데이터를 불러오는 중 오류 발생: " + e.getMessage());
        }
    }

    private double gridToLatitude(int nx) {
        return 38.0 - (nx - 60) * 0.01;  // 예제 변환 값 (실제 변환 방식 적용 필요)
    }

    private double gridToLongitude(int ny) {
        return 125.0 + (ny - 127) * 0.01;  // 예제 변환 값 (실제 변환 방식 적용 필요)
    }

    public String findClosestCity(double userLat, double userLon) {
        String closestCity = null;
        double minDistance = Double.MAX_VALUE;

        for (Map.Entry<String, Integer[]> entry : cityCoordinates.entrySet()) {
            String city = entry.getKey();
            Integer[] grid = entry.getValue();
            double lat = gridToLatitude(grid[0]);
            double lon = gridToLongitude(grid[1]);
            double distance = haversine(userLat, userLon, lat, lon);

            if (distance < minDistance) {
                minDistance = distance;
                closestCity = city;
            }
        }

        return closestCity;
    }

    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }


    public Map<String, String> getFormattedWeatherData(String city) {
        Map<String, Object> weatherData = getWeatherData(city);

        if (weatherData == null || !weatherData.containsKey("response")) {
            return Map.of("error", "날씨 데이터를 가져오지 못했습니다.");
        }

        Map<String, String> formattedData = new LinkedHashMap<>();
        try {
            Map responseBody = (Map) ((Map) weatherData.get("response")).get("body");
            List<Map<String, Object>> items = (List<Map<String, Object>>) ((Map) responseBody.get("items")).get("item");

            for (Map<String, Object> item : items) {
                String category = (String) item.get("category");
                String value = item.get("obsrValue").toString();
                switch (category) {
                    case "T1H":
                        formattedData.put("temperature", value + "°C");
                        break;
                    case "REH":
                        formattedData.put("humidity", value + "%");
                        break;
                    case "WSD":
                        formattedData.put("windSpeed", value + " m/s");
                        break;
                    case "PTY":
                        formattedData.put("precipitationType", getPrecipitationType(value));
                        break;
                    default:
                        formattedData.put(category, value);
                        break;
                }
            }
        } catch (Exception e) {
            System.err.println("🚨 데이터 파싱 중 오류 발생: " + e.getMessage());
            return Map.of("error", "날씨 데이터 파싱 중 오류가 발생했습니다.");
        }
        return formattedData;
    }

    private String getPrecipitationType(String value) {
        switch (value) {
            case "0": return "No precipitation";
            case "1": return "Rain";
            case "2": return "Rain/Snow mixed";
            case "3": return "Snow";
            case "4": return "Shower";
            default: return "Unknown";
        }
    }

    private Map<String, Object> getWeatherData(String city) {
        String baseDate = new SimpleDateFormat("yyyyMMdd").format(new Date());
        String baseTime = getLatestBaseTime();

        Integer[] coordinates = cityCoordinates.getOrDefault(city, new Integer[]{60, 127});
        int nx = coordinates[0];
        int ny = coordinates[1];

        String encodedServiceKey = URLEncoder.encode(SERVICE_KEY, StandardCharsets.UTF_8);

        URI uri = UriComponentsBuilder.fromHttpUrl(API_URL)
                .queryParam("serviceKey", encodedServiceKey)
                .queryParam("dataType", "JSON")
                .queryParam("numOfRows", 10)
                .queryParam("pageNo", 1)
                .queryParam("base_date", baseDate)
                .queryParam("base_time", baseTime)
                .queryParam("nx", nx)
                .queryParam("ny", ny)
                .build(true)
                .toUri();

        try {
            return restTemplate.getForObject(uri, Map.class);
        } catch (Exception e) {
            System.err.println("🚨 API 호출 중 오류 발생: " + e.getMessage());
            return Map.of("error", "날씨 데이터를 가져오지 못했습니다.");
        }
    }

    private String getLatestBaseTime() {
        String[] availableTimes = {"0200", "0500", "0800", "1100", "1400", "1700", "2000", "2300"};
        SimpleDateFormat sdf = new SimpleDateFormat("HHmm");
        int now = Integer.parseInt(sdf.format(new Date()));

        String latestTime = "0200";
        for (String time : availableTimes) {
            if (now >= Integer.parseInt(time)) {
                latestTime = time;
            } else {
                break;
            }
        }
        return latestTime;
    }
}
