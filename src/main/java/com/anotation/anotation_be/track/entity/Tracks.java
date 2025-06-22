package com.anotation.anotation_be.track.entity;

import com.anotation.anotation_be.common.enums.EmotionEnum;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "tbl_tracks")
public class Tracks {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String spotifyId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String artist;

    @Column(nullable = false)
    private String album;

    @Column(nullable = false)
    private String thumbnail;

    @Column(nullable = false)
    private int popularity;

    @Column(nullable = false)
    private String releaseDate;
//
//    @Column(nullable = false)
//    @Enumerated(EnumType.STRING)
//    @ColumnDefault("'NEUTRAL'")
//    private EmotionEnum emotion;
}
