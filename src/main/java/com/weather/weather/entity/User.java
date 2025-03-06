package com.weather.weather.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String provider;
    private String providerId;
    private String username;
    private String email;
    private String nickname;
    private String profileImage;

    @Builder
    public User(String profileImage, String email, Long id, String provider, String providerId, String username, String nickname) {
        this.profileImage = profileImage;
        this.email = email;
        this.id = id;
        this.provider = provider;
        this.providerId = providerId;
        this.username = username;
        this.nickname = nickname;
    }

}