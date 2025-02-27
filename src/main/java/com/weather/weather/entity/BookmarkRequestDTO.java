package com.weather.weather.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BookmarkRequestDTO {
    private Long userId;
    private String location;
}
