package com.anotation.anotation_be.track.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RedisTrackIndexDto {
    private String email;
    private int index;
}
