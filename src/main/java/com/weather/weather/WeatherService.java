package com.weather.weather;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class WeatherService {

    private static final String API_URL = "http://apis.data.go.kr/1360000/VilageFcstInfoService_2.0/getUltraSrtNcst";
    private static final String SERVICE_KEY = "hNQQGlEEAxRJBkdL1VFgEmEDkpw5QWrK0cK3BJ0lx0mHufYq3ruuhwSS4uosgUFQDKNOOMB2fWW0iiQWJb76GA==";

    @Autowired
    private RestTemplate restTemplate;

    // 기존 getWeatherData 메서드
    public Map<String, Object> getWeatherData(String city) {
        String baseDate = new SimpleDateFormat("yyyyMMdd").format(new Date());
        String baseTime = getLatestBaseTime();

        Map<String, Integer[]> cityCoordinates = new HashMap<>();
        cityCoordinates.put("서울", new Integer[]{60, 127});
        cityCoordinates.put("부산", new Integer[]{98, 76});
        cityCoordinates.put("대구", new Integer[]{89, 90});
        cityCoordinates.put("광주", new Integer[]{58, 74});
        cityCoordinates.put("대전", new Integer[]{67, 100});
        cityCoordinates.put("제주", new Integer[]{52, 38});

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

    // 새롭게 추가된 getFormattedWeatherData 메서드
    public Map<String, String> getFormattedWeatherData(String city) {
        Map<String, Object> weatherData = getWeatherData(city);

        if (weatherData == null || !weatherData.containsKey("response")) {
            throw new RuntimeException("🚨 API 응답 데이터가 올바르지 않습니다.");
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
                        formattedData.put("Temperature (°C)", value);
                        break;
                    case "REH":
                        formattedData.put("Humidity (%)", value);
                        break;
                    case "WSD":
                        formattedData.put("Wind Speed (m/s)", value);
                        break;
                    case "PTY":
                        formattedData.put("Precipitation Type", value);
                        break;
                    default:
                        formattedData.put(category, value);
                        break;
                }
            }
        } catch (Exception e) {
            System.err.println("🚨 데이터를 파싱하는 중 오류 발생: " + e.getMessage());
        }
        return formattedData;
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
