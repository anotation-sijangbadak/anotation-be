package com.anotation.anotation_be.emotion.repo;

import com.anotation.anotation_be.emotion.entity.Emotions;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmotionRepository extends JpaRepository<Emotions, Long> {
}
