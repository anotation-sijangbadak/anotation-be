package com.anotation.anotation_be.user.repo;

import com.anotation.anotation_be.user.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuthRepository extends JpaRepository<Users, Long> {

    boolean existsByEmail(String email);

    boolean existsByNickname(String nickname);

    Optional<Users> findByEmail(String email);
}