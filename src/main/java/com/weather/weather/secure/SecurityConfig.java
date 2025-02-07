package com.weather.weather.secure;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // CSRF 비활성화 (필요 시)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/login", "/oauth2/**").permitAll() // 로그인 관련 요청 허용
                        .anyRequest().authenticated() // 나머지는 인증 필요
                )
                .oauth2Login(oauth2 -> oauth2
                        .defaultSuccessUrl("/main", true) // 로그인 성공 후 리디렉션
                );

        return http.build();
    }
}