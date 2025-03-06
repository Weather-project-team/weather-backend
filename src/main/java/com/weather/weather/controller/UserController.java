package com.weather.weather.controller;

import com.weather.weather.entity.User;
import com.weather.weather.repository.UserRepository;
import com.weather.weather.securiry.CustomOAuth2User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository; // User 조회를 위한 Repository

    @GetMapping("/me")
    public ResponseEntity<?> getUser(@AuthenticationPrincipal CustomOAuth2User user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // providerId를 이용해 User 테이블에서 ID 조회
        Optional<User> userEntity = userRepository.findByProviderAndProviderId(user.getProvider(), user.getProviderId());

        if (userEntity.isPresent()) {
            return ResponseEntity.ok(userEntity.get());
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }
    }
}