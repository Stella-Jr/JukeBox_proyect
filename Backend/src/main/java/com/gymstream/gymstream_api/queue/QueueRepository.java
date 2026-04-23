package com.gymstream.gymstream_api.queue;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface QueueRepository extends JpaRepository<QueueItem, Long> {

    // Busca todos los items de una sala con un status específico
    // Spring genera el SQL automáticamente leyendo el nombre del método:
    // SELECT * FROM queue WHERE room_id = ? AND status = ?
    List<QueueItem> findByRoomIdAndStatus(Long roomId, QueueItem.QueueStatus status);

    // Busca un item específico por sala, canción y status
    // Esto nos permite saber si una canción ya está en la cola
    // SELECT * FROM queue WHERE room_id = ? AND song_id = ? AND status = ?
    Optional<QueueItem> findByRoomIdAndSongIdAndStatus(
        Long roomId, Long songId, QueueItem.QueueStatus status
    );
}