package com.weather.weather.securiry;

import com.weather.weather.entity.User;
import com.weather.weather.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);

        String provider = userRequest.getClientRegistration().getRegistrationId(); // google, kakao
        String providerId = getProviderId(oauth2User, provider);
        String nickname = getNickname(oauth2User, provider);
        String profileImage = getProfileImage(oauth2User, provider);

        User user = userRepository.findByProviderAndProviderId(provider, providerId)
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .provider(provider)
                            .providerId(providerId)
                            .nickname(nickname)
                            .profileImage(profileImage)
                            .build();
                    return userRepository.save(newUser);
                });

        return new CustomOAuth2User(oauth2User, provider, providerId);
    }

    private String getProviderId(OAuth2User oauth2User, String provider) {
        if ("google".equals(provider)) {
            return oauth2User.getAttribute("sub");
        } else if ("kakao".equals(provider)) {
            return oauth2User.getAttribute("id").toString();
        }
        return null;
    }

    private String getNickname(OAuth2User oauth2User, String provider) {
        if ("google".equals(provider)) {
            return oauth2User.getAttribute("name");
        } else if ("kakao".equals(provider)) {
            Map<String, Object> properties = oauth2User.getAttribute("properties");
            return properties != null ? properties.get("nickname").toString() : "unknown";
        }
        return "unknown";
    }

    private String getProfileImage(OAuth2User oauth2User, String provider) {
        if ("google".equals(provider)) {
            return oauth2User.getAttribute("picture");
        } else if ("kakao".equals(provider)) {
            Map<String, Object> properties = oauth2User.getAttribute("properties");
            return properties != null ? properties.get("profile_image").toString() : null;
        }
        return null;
    }
}
