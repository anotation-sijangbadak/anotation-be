package com.anotation.anotation_be.snap.entity;

import com.anotation.anotation_be.common.entity.BaseTimeEntity;
import com.anotation.anotation_be.emotion.entity.Traces;
import com.anotation.anotation_be.track.entity.Tracks;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "tbl_snaps")
public class Snaps extends BaseTimeEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trace_id")
    private Traces trace;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "track_id")
    private Tracks track;

    @Column(length = 1000, nullable = true)
    private String description;
}
