package com.anotation.anotation_be.track.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import javax.sound.midi.Track;

public interface TrackRepository extends JpaRepository<Track, Long> {

}
