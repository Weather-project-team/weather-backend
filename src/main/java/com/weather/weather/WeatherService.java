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

    public List<String> getCityNames() {
        List<String> cityList = new ArrayList<>(cityCoordinates.keySet());
        Collections.sort(cityList); // 가나다순 정렬
        return cityList;
    }

    // 기상청 좌표 변환을 위한 상수 추가
    private static final double RE = 6371.00877; // 지구 반경(km)
    private static final double GRID = 5.0; // 격자 간격(km)
    private static final double SLAT1 = 30.0; // 표준 위도 1
    private static final double SLAT2 = 60.0; // 표준 위도 2
    private static final double OLON = 126.0; // 기준점 경도
    private static final double OLAT = 38.0; // 기준점 위도
    private static final double XO = 43; // 기준점 X 좌표
    private static final double YO = 136; // 기준점 Y 좌표


    private double[] gridToLatitudeLongitude(int nx, int ny) {
        double DEGRAD = Math.PI / 180.0;
        double RADDEG = 180.0 / Math.PI;

        double re = RE / GRID;
        double slat1 = SLAT1 * DEGRAD;
        double slat2 = SLAT2 * DEGRAD;
        double olon = OLON * DEGRAD;
        double olat = OLAT * DEGRAD;

        double sn = Math.tan(Math.PI * 0.25 + slat2 * 0.5) / Math.tan(Math.PI * 0.25 + slat1 * 0.5);
        sn = Math.log(Math.cos(slat1) / Math.cos(slat2)) / Math.log(sn);
        double sf = Math.pow(Math.tan(Math.PI * 0.25 + slat1 * 0.5), sn) * Math.cos(slat1) / sn;
        double ro = Math.pow(Math.tan(Math.PI * 0.25 + olat * 0.5), -sn) * sf * re;
        double x = nx - XO;
        double y = ro - ny + YO;

        double ra = Math.sqrt(x * x + y * y);
        double theta = Math.atan2(x, y);
        double alat = Math.pow((re * sf / ra), (1.0 / sn));
        alat = 2.0 * Math.atan(alat) - Math.PI * 0.5;
        double alon = theta / sn + olon;

        return new double[]{alat * RADDEG, alon * RADDEG};
    }

    // ✅ 현재 위치(위도, 경도)와 가장 가까운 도시 찾기
    public String findClosestCity(double userLat, double userLon) {
        String closestCity = null;
        double minDistance = Double.MAX_VALUE;

        for (Map.Entry<String, Integer[]> entry : cityCoordinates.entrySet()) {
            String city = entry.getKey();
            Integer[] grid = entry.getValue();

            // ✅ 격자 좌표를 위경도로 변환
            double[] latLon = gridToLatitudeLongitude(grid[0], grid[1]);
            double lat = latLon[0];
            double lon = latLon[1];

            double distance = haversine(userLat, userLon, lat, lon);

            if (distance < minDistance) {
                minDistance = distance;
                closestCity = city;
            }
        }

        return closestCity;
    }

    // ✅ 하버사인 공식 (거리 계산)
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
        String skyCondition = "";
        String precipitationType = "";

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
                        precipitationType = getPrecipitationType(value);
                        formattedData.put("precipitationType", precipitationType); // ✅ PTY 추가
                        break;
                    case "SKY":
                        skyCondition = getSkyCondition(value);
                        formattedData.put("skyCondition", skyCondition);
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


    private String getSkyCondition(String value) {
        switch (value) {
            case "1": return "맑음";  // Clear
            case "3": return "구름많음"; // Partly Cloudy
            case "4": return "흐림";  // Cloudy
            default: return "알 수 없음"; // Unknown
        }
    }

    private String getPrecipitationType(String value) {
        switch (value) {
            case "0": return "강수 없음";
            case "1": return "비";
            case "2": return "비/눈";
            case "3": return "눈";
            case "4": return "소나기";
            case "5": return "빗방울";
            case "6": return "빗방울/눈날림";
            case "7": return "눈날림";
            default: return "알 수 없음";
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
