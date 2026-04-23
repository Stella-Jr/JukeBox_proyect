package com.gymstream.gymstream_api.vote;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface VoteRepository extends JpaRepository<Vote, Long> {

    // Busca un voto específico de un usuario en un item de la cola
    // Nos permite saber si el usuario ya votó esa canción
    // SELECT * FROM votes WHERE queue_id = ? AND user_id = ?
    Optional<Vote> findByQueueItemIdAndUserId(Long queueItemId, Long userId);

    // Cuenta cuántos votos tiene un item de la cola
    // SELECT COUNT(*) FROM votes WHERE queue_id = ?
    int countByQueueItemId(Long queueItemId);
}