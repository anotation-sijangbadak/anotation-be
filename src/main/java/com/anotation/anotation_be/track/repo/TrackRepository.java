package com.anotation.anotation_be.track.repo;

import com.anotation.anotation_be.track.entity.Tracks;
import org.springframework.data.jpa.repository.JpaRepository;

import javax.sound.midi.Track;
import java.util.List;
import java.util.Optional;

public interface TrackRepository extends JpaRepository<Tracks, Long> {

    Optional<Tracks> findBySpotifyId(String spotifyId);
}
