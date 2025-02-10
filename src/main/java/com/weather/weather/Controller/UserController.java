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
        Map<String, Object> userInfo = Map.of(
                "name", user.getAttribute("name"),
                "email", user.getAttribute("email"),
                "picture", user.getAttribute("picture")
        );

        return ResponseEntity.ok(userInfo);
    }
}