package com.anotation.anotation_be.emotion.entity;

import com.anotation.anotation_be.auth.entity.Users;
import com.anotation.anotation_be.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "tbl_traces")
public class Traces extends BaseTimeEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private Users user;

    @Column(nullable = false)
    private String prompt;

    @OneToMany(cascade = CascadeType.PERSIST, orphanRemoval = true)
    private List<Emotions> emotions = new ArrayList<>();
}
