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

    private static final String API_URL = "http://apis.data.go.kr/1360000/VilageFcstInfoService_2.0/getUltraSrtFcst";
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

        // ✅ **기존의 (nx - XO), (ro - ny + YO) 계산을 정확하게 수정**
        double x = nx - XO;
        double y = ro - (ny - YO);

        double ra = Math.sqrt(x * x + y * y);
        double theta = Math.atan2(x, y);
        double alat = Math.pow((re * sf / ra), (1.0 / sn));
        alat = 2.0 * Math.atan(alat) - Math.PI * 0.5;
        double alon = theta / sn + olon;

        System.out.println("✅ 변환된 위경도 (수정 후): 위도=" + (alat * RADDEG) + ", 경도=" + (alon * RADDEG));
        return new double[]{alat * RADDEG, alon * RADDEG};
    }


    // ✅ 현재 위치(위도, 경도)와 가장 가까운 도시 찾기
    public String findClosestCity(double userLat, double userLon) {
        int[] userGrid = latitudeLongitudeToGrid(userLat, userLon);

        // 🚀 GPS 오차 보정을 적용하여 가장 가까운 도시 찾기
        String closestCity = adjustCoordinatesToNearestGrid(userGrid[0], userGrid[1]);

        System.out.println("✅ [최종] 선택된 가장 가까운 도시: " + closestCity);
        return closestCity;
    }


    private int[] latitudeLongitudeToGrid(double lat, double lon) {
        double DEGRAD = Math.PI / 180.0;
        double re = RE / GRID;
        double slat1 = SLAT1 * DEGRAD;
        double slat2 = SLAT2 * DEGRAD;
        double olon = OLON * DEGRAD;
        double olat = OLAT * DEGRAD;

        double sn = Math.tan(Math.PI * 0.25 + slat2 * 0.5) / Math.tan(Math.PI * 0.25 + slat1 * 0.5);
        sn = Math.log(Math.cos(slat1) / Math.cos(slat2)) / Math.log(sn);
        double sf = Math.pow(Math.tan(Math.PI * 0.25 + slat1 * 0.5), sn) * Math.cos(slat1) / sn;
        double ro = Math.pow(Math.tan(Math.PI * 0.25 + olat * 0.5), -sn) * sf * re;
        double ra = Math.pow(Math.tan(Math.PI * 0.25 + lat * DEGRAD * 0.5), -sn) * sf * re;
        double theta = lon * DEGRAD - olon;
        if (theta > Math.PI) theta -= 2.0 * Math.PI;
        if (theta < -Math.PI) theta += 2.0 * Math.PI;
        theta *= sn;

        // ✅ **여기에서 반올림 처리 (`Math.round()`)로 격자 좌표 정밀도 개선**
        int x = (int) Math.round(ra * Math.sin(theta) + XO);
        int y = (int) Math.round(ro - ra * Math.cos(theta) + YO);

        System.out.println("✅ 변환된 격자 좌표 (수정 후): nx=" + x + ", ny=" + y);
        return new int[]{x, y};
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
                String value = item.get("fcstValue").toString();
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
                .queryParam("numOfRows", 50)
                .queryParam("pageNo", 1)
                .queryParam("base_date", baseDate)
                .queryParam("base_time", baseTime)
                .queryParam("nx", nx)
                .queryParam("ny", ny)
                .build(true)
                .toUri();

        try {
            Map<String, Object> response = restTemplate.getForObject(uri, Map.class);

            // ✅ SKY 데이터가 포함되는지 콘솔에 출력
            System.out.println("🔍 기상청 API 응답 데이터: " + response);

            return response;
        } catch (Exception e) {
            System.err.println("🚨 API 호출 중 오류 발생: " + e.getMessage());
            return Map.of("error", "날씨 데이터를 가져오지 못했습니다.");
        }
    }

    private String adjustCoordinatesToNearestGrid(int nx, int ny) {
        String closestCity = null;
        double minDistance = Double.MAX_VALUE;
        boolean isSameDistance = false; // 🚀 거리 같은 경우 우선순위 적용

        System.out.println("🚀 [디버깅] 현재 GPS 변환된 좌표: nx=" + nx + ", ny=" + ny);

        for (Map.Entry<String, Integer[]> entry : cityCoordinates.entrySet()) {
            int gridNx = entry.getValue()[0];
            int gridNy = entry.getValue()[1];
            String city = entry.getKey();

            // ✅ 현재 좌표(nx, ny)에서 ±2 이상 차이나면 비교 대상에서 제외
            if (Math.abs(nx - gridNx) > 2 || Math.abs(ny - gridNy) > 2) {
                continue;
            }

            // 🚀 유클리드 거리 계산
            double distance = Math.sqrt(Math.pow(nx - gridNx, 2) + Math.pow(ny - gridNy, 2));

            System.out.println("🔍 [비교 대상] " + city + " | JSON 좌표: (" + gridNx + ", " + gridNy + ") | 거리: " + distance);

            // ✅ 더 가까운 도시 찾기 (거리 비교)
            if (distance < minDistance) {
                minDistance = distance;
                closestCity = city;
                isSameDistance = false; // 새로운 최소 거리 발견 -> 동일 거리 우선순위 해제
            }
            // ✅ 거리가 같을 경우, 우선순위 적용
            else if (distance == minDistance) {
                // 1️⃣ nx 값이 같으면 ny 차이가 적은 곳 선택
                if (gridNx == nx && Math.abs(ny - gridNy) < Math.abs(ny - Integer.parseInt(closestCity.split(" ")[2]))) {
                    closestCity = city;
                    isSameDistance = true;
                }
                // 2️⃣ nx, ny 모두 같으면 기존 선택 유지
            }
        }

        System.out.println("🎯 [결과] 선택된 가장 가까운 도시: " + closestCity);
        return closestCity;
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
