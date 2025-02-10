package com.weather.weather;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@Controller
public class WeatherController {

    @Autowired
    private WeatherService weatherService;

    @GetMapping("/weather")
    public String showWeatherPage(@RequestParam(required = false) String city, Model model) {
        if (city == null || city.isEmpty()) {
            model.addAttribute("error", "도시명을 입력하세요.");
            return "weather";
        }

        try {
            Map<String, String> formattedWeatherData = weatherService.getFormattedWeatherData(city);
            model.addAttribute("city", city);
            model.addAttribute("weather", formattedWeatherData);
        } catch (Exception e) {
            model.addAttribute("error", "날씨 데이터를 가져오지 못했습니다.");
        }

        return "weather";
    }
}
