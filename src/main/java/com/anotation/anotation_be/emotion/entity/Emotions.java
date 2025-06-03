package com.anotation.anotation_be.emotion.entity;

import com.anotation.anotation_be.common.enums.Active;
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
@Table(name = "tbl_emotions")
public class Emotions {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trace_id")
    private Traces trace;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @ColumnDefault("'NEUTRAL'")
    private EmotionEnum emotion;
}
