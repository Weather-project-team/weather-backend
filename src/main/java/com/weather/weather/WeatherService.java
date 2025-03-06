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


    public String findClosestCity(double userLat, double userLon) {
        int[] userGrid = latitudeLongitudeToGrid(userLat, userLon);

        System.out.println("✅ [GPS 확인] 받은 위도: " + userLat + ", 경도: " + userLon);
        System.out.println("✅ [격자 변환] 변환된 nx: " + userGrid[0] + ", ny: " + userGrid[1]);

        // 🚀 가장 가까운 도시 찾기
        String closestCity = adjustCoordinatesToNearestGrid(userGrid[0], userGrid[1]);
        System.out.println("🎯 [결과] 최종 선택된 도시: " + closestCity);

        return closestCity;
    }


    private String adjustCoordinatesToNearestGrid(int nx, int ny) {
        System.out.println("🚀 [도시 매칭] 현재 격자 좌표: nx=" + nx + ", ny=" + ny);

        String closestCity = null;
        double minDistance = Double.MAX_VALUE;

        for (Map.Entry<String, Integer[]> entry : cityCoordinates.entrySet()) {
            int gridNx = entry.getValue()[0];
            int gridNy = entry.getValue()[1];
            String city = entry.getKey();

            double distance = Math.sqrt(Math.pow(nx - gridNx, 2) + Math.pow(ny - gridNy, 2));

            if (distance < minDistance) {
                minDistance = distance;
                closestCity = city;
            }
        }
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

        int x = (int) Math.round(ra * Math.sin(theta) + XO);
        int y = (int) Math.round(ro - ra * Math.cos(theta) + YO);

        // ✅ 좌표 변환 결과 확인 로그 추가
        System.out.println("✅ [좌표 변환] 위도: " + lat + ", 경도: " + lon + " → 격자 nx: " + x + ", ny: " + y);

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

        try {
            Map responseBody = (Map) ((Map) weatherData.get("response")).get("body");
            List<Map<String, Object>> items = (List<Map<String, Object>>) ((Map) responseBody.get("items")).get("item");

            // ✅ 최신 예보 데이터만 저장 (각 category당 가장 가까운 시간 데이터 1개만 유지)
            Map<String, Map<String, Object>> latestData = new HashMap<>();

            for (Map<String, Object> item : items) {
                String category = (String) item.get("category");
                String fcstTime = item.get("fcstTime").toString();

                if (!latestData.containsKey(category) || fcstTime.compareTo(latestData.get(category).get("fcstTime").toString()) < 0) {
                    latestData.put(category, item);
                }
            }

            // ✅ 중복 제거 후 정리
            for (Map.Entry<String, Map<String, Object>> entry : latestData.entrySet()) {
                String category = entry.getKey();
                String value = entry.getValue().get("fcstValue").toString();

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
                    case "SKY":
                        formattedData.put("skyCondition", getSkyCondition(value));
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
        boolean usePreviousDay = false; // 🚀 처음에는 현재 날짜 사용
        String baseTime = getLatestBaseTime();
        String baseDate = getBaseDate(baseTime, usePreviousDay);

        Integer[] coordinates = cityCoordinates.getOrDefault(city, new Integer[]{60, 127});
        int nx = coordinates[0];
        int ny = coordinates[1];

        String encodedServiceKey = URLEncoder.encode(SERVICE_KEY, StandardCharsets.UTF_8);

        // ✅ 1️⃣ 최초 요청
        Map<String, Object> response = requestWeatherData(baseDate, baseTime, nx, ny, encodedServiceKey);

        // 🚨 2️⃣ `NO_DATA` 발생 시 `base_time` 조정 후 재요청
        if (response == null || !isValidResponse(response)) {
            System.out.println("🚨 NO DATA! Trying an earlier base_time...");
            baseTime = getAdjustedBaseTime(baseTime);
            response = requestWeatherData(baseDate, baseTime, nx, ny, encodedServiceKey);
        }

        // 🚨 3️⃣ `NO_DATA` 발생 시 `base_date` 하루 전으로 바꿔서 재요청
        if (response == null || !isValidResponse(response)) {
            System.out.println("🚨 NO DATA! Trying previous day's data...");
            usePreviousDay = true;
            baseDate = getBaseDate(baseTime, usePreviousDay);
            response = requestWeatherData(baseDate, baseTime, nx, ny, encodedServiceKey);
        }

        // 🚨 4️⃣ `NO_DATA` 발생 시 주변 좌표로 재요청
        if (response == null || !isValidResponse(response)) {
            System.out.println("🚨 API NO DATA! Retrying with adjusted coordinates...");
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    if (dx == 0 && dy == 0) continue; // 이미 요청한 값은 제외
                    int newNx = nx + dx;
                    int newNy = ny + dy;
                    System.out.println("🔄 재요청: nx=" + newNx + ", ny=" + newNy);

                    response = requestWeatherData(baseDate, baseTime, newNx, newNy, encodedServiceKey);

                    if (response != null && isValidResponse(response)) {
                        System.out.println("✅ [성공] 기상청 API 데이터 확보 (nx=" + newNx + ", ny=" + newNy + ")");
                        return response; // 유효한 데이터가 있으면 반환
                    }
                }
            }
        }

        return Map.of("error", "기상청 API에 해당 좌표의 데이터가 없습니다.");
    }



    private Map<String, Object> requestWeatherData(String baseDate, String baseTime, int nx, int ny, String encodedServiceKey) {
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

            if (response == null || !response.containsKey("response")) {
                System.out.println("🚨 기상청 API NO_DATA (nx=" + nx + ", ny=" + ny + ")");
                return Map.of("error", "NO_DATA");
            }

            System.out.println("🔍 기상청 API 응답 데이터 (nx=" + nx + ", ny=" + ny + ", baseTime=" + baseTime + "): " + response);
            return response;
        } catch (Exception e) {
            System.err.println("🚨 API 호출 중 오류 발생: " + e.getMessage());
            return Map.of("error", "날씨 데이터를 가져오지 못했습니다.");
        }
    }



    private String getLatestBaseTime() {
        String[] availableTimes = {"2300", "2000", "1700", "1400", "1100", "0800", "0500", "0200"};
        SimpleDateFormat sdf = new SimpleDateFormat("HHmm");
        int now = Integer.parseInt(sdf.format(new Date()));

        for (String time : availableTimes) {
            if (now >= Integer.parseInt(time)) {
                System.out.println("⏰ 선택된 base_time: " + time);
                return time;
            }
        }

        // 기본값 (오전 2시 이전이라면 전날 23:00 데이터 사용)
        System.out.println("⏰ 현재 시간이 02:00 이전이므로 전날 23:00 데이터 사용");
        return "2300";
    }


    private String getBaseDate(String baseTime, boolean usePreviousDay) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        Calendar calendar = Calendar.getInstance();

        // ✅ 만약 `usePreviousDay`가 true이면 하루 전으로 설정
        if (usePreviousDay) {
            calendar.add(Calendar.DATE, -1);
            System.out.println("🗓️ 전날 데이터로 변경: " + dateFormat.format(calendar.getTime()));
        }

        return dateFormat.format(calendar.getTime());
    }

    private boolean isValidResponse(Map<String, Object> response) {
        if (response == null || !response.containsKey("response")) {
            return false;
        }

        Map<String, Object> responseBody = (Map<String, Object>) response.get("response");
        if (responseBody == null || !responseBody.containsKey("body")) {
            return false;
        }

        return true;
    }

    private String getAdjustedBaseTime(String currentBaseTime) {
        String[] availableTimes = {"2300", "2000", "1700", "1400", "1100", "0800", "0500", "0200"};

        for (int i = availableTimes.length - 1; i >= 0; i--) {
            if (availableTimes[i].equals(currentBaseTime) && i > 0) {
                System.out.println("⏪ `NO_DATA`, 이전 base_time 사용: " + availableTimes[i - 1]);
                return availableTimes[i - 1]; // 이전 base_time 반환
            }
        }

        // 만약 더 이상 이전 시간이 없으면 가장 마지막 시간(`2300`) 사용
        System.out.println("⏪ `NO_DATA`, 사용 가능한 base_time 없음 → 기본값 2300 사용");
        return "2300";
    }


}
