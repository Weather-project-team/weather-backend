package com.weather.weather.Controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
        }

        OAuth2User user = (OAuth2User) authentication.getPrincipal();
        String provider = user.getAttribute("iss") != null ? "google" : "kakao";  // Provider 구분

        Map<String, Object> userInfo;
        if ("google".equals(provider)) {
            userInfo = Map.of(
                    "name", user.getAttribute("name"),         // 사용자 이름
                    "email", user.getAttribute("email"),       // 이메일
                    "picture", user.getAttribute("picture")    // 프로필 사진
            );
        } else {
            Map<String, Object> kakaoAccount = (Map<String, Object>) user.getAttribute("kakao_account");
            Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");

            userInfo = Map.of(
                    "nickname", profile.get("nickname"),                 // 카카오 닉네임
                    "profile_image", profile.get("profile_image_url")    // 프로필 사진
            );
        }

        return ResponseEntity.ok(userInfo);
    }
}