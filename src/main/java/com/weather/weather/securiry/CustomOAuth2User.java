package com.weather.weather.securiry;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Map;

public class CustomOAuth2User implements OAuth2User {

    private final OAuth2User oauth2User;
    private final String provider;
    private final String providerId;

    public CustomOAuth2User(OAuth2User oauth2User, String provider, String providerId) {
        this.oauth2User = oauth2User;
        this.provider = provider;
        this.providerId = providerId;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return oauth2User.getAttributes();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return oauth2User.getAuthorities();
    }

    @Override
    public String getName() {
        return providerId; // providerId를 식별자로 사용
    }

    public String getProvider() {
        return provider;
    }

    public String getProviderId() {
        return providerId;
    }

    public String getNickname() {
        return oauth2User.getAttribute("name");
    }

    public String getProfileImage() {
        return oauth2User.getAttribute("picture");
    }
}