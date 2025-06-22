package com.anotation.anotation_be.common.dto.track;

import lombok.*;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class TrackInfoDto {
    String spotifyId;
    String title;
    String artist;
    String album;
    String thumbnail;
    int popularity;
    String releaseDate;

    int index;
}
