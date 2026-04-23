package com.gymstream.gymstream_api.song;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SongRepository extends JpaRepository<Song, Long> {

    // Busca una canción por su ID de YouTube
    // Usamos esto para no duplicar canciones en la tabla songs
    Optional<Song> findByYoutubeId(String youtubeId);
}