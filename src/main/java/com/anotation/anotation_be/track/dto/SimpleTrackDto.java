package com.anotation.anotation_be.track.dto;

import lombok.*;

import java.io.Serializable;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class SimpleTrackDto {
    private String title;
    private String artist;
    private String reason;
}
