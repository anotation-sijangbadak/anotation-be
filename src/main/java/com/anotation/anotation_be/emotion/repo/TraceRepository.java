package com.anotation.anotation_be.emotion.repo;

import com.anotation.anotation_be.emotion.entity.Traces;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TraceRepository extends JpaRepository<Traces, Long> {
}
