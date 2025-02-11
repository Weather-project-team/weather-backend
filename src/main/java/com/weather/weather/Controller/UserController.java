package com.weather.weather.Controller;

import com.weather.weather.entity.User;
import com.weather.weather.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
        }

        DefaultOAuth2User oauth2User = (DefaultOAuth2User) authentication.getPrincipal();

        // 고유 식별자 가져오기
        String providerId = getProviderId(oauth2User);
        String provider = oauth2User.getAttribute("sub") != null ? "google" : "kakao";

        // User 엔티티에서 provider와 providerId로 사용자 검색
        Optional<User> optionalUser = userRepository.findByProviderAndProviderId(provider, providerId);
        User user = optionalUser.orElseGet(() -> saveNewUser(oauth2User, provider, providerId));

        // 사용자 정보 반환
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("name", user.getName());
        userInfo.put("email", user.getEmail() != null ? user.getEmail() : "No email provided");
        userInfo.put("nickname", user.getNickname() != null ? user.getNickname() : user.getName());
        userInfo.put("profileImage", user.getProfileImage());
        userInfo.put("provider", user.getProvider());

        return ResponseEntity.ok(userInfo);
    }

    private String getProviderId(DefaultOAuth2User oauth2User) {
        if (oauth2User.getAttribute("sub") != null) {
            return oauth2User.getAttribute("sub");  // Google 사용자 ID
        }
        Object kakaoId = oauth2User.getAttribute("id");
        if (kakaoId instanceof Long) {
            return String.valueOf(kakaoId);  // Kakao 사용자 ID
        }
        throw new RuntimeException("Unsupported provider or unexpected ID type");
    }

    private User saveNewUser(DefaultOAuth2User oauth2User, String provider, String providerId) {
        User newUser = new User();
        newUser.setProvider(provider);
        newUser.setProviderId(providerId);

        if (provider.equals("google")) {
            newUser.setName(oauth2User.getAttribute("name"));
            newUser.setEmail(oauth2User.getAttribute("email"));
            newUser.setProfileImage(oauth2User.getAttribute("picture"));
        } else if (provider.equals("kakao")) {
            Map<String, Object> kakaoAccount = (Map<String, Object>) oauth2User.getAttribute("kakao_account");
            Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
            newUser.setName((String) profile.get("nickname"));
            newUser.setProfileImage((String) profile.get("profile_image_url"));

            if (kakaoAccount.containsKey("email")) {
                newUser.setEmail((String) kakaoAccount.get("email"));
            }
        }
        return userRepository.save(newUser);
    }
}