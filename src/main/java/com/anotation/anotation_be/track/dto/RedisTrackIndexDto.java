package com.anotation.anotation_be.track.dto;

import com.anotation.anotation_be.common.enums.EmotionEnum;
import com.anotation.anotation_be.common.enums.Genre;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class RedisTrackIndexDto {
    private String email;
    private int index;

}
