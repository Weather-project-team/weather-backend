package com.weather.weather.Controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/main")
public class MainController {

    @GetMapping
    public ResponseEntity<String> mainPage() {
        return ResponseEntity.ok("OAuth2 구글 로그인 성공! 메인 페이지입니다.");
    }
}