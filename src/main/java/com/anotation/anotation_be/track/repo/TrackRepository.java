package com.anotation.anotation_be.track.repo;

import com.anotation.anotation_be.track.entity.Tracks;
import org.springframework.data.jpa.repository.JpaRepository;

import javax.sound.midi.Track;

public interface TrackRepository extends JpaRepository<Tracks, Long> {

}
