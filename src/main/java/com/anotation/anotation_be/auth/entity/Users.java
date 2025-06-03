package com.anotation.anotation_be.auth.entity;

import com.anotation.anotation_be.common.entity.BaseTimeEntity;
import com.anotation.anotation_be.common.enums.Active;
import com.anotation.anotation_be.common.enums.Role;
import com.anotation.anotation_be.emotion.entity.Traces;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.DynamicInsert;

import java.util.List;

@Entity
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
@DynamicInsert
@Table(name = "tbl_users")
public class Users extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true)
    private String nickname;

    @Column(nullable = false)
    private Long genre;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @ColumnDefault("'USER'")
    private Role role;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @ColumnDefault("'ENABLED'")
    private Active active;

    @OneToMany(orphanRemoval = true)
    private List<Traces> traces;


    public void setGenre(Long genre) {
        if (genre != null && genre > 0) {
            this.genre = genre;
        }
    }

    public void setNickname(String nickname) {
        if (nickname != null && !nickname.isBlank()) {
            this.nickname = nickname;
        }
    }
}