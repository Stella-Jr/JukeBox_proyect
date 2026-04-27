package com.gymstream.gymstream_api.vote;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.Optional;

public interface VoteRepository extends JpaRepository<Vote, Long> {

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM Vote v WHERE v.queueItem.id = :queueItemId")
    void deleteByQueueItemId(@Param("queueItemId") Long queueItemId);

    // Busca un voto específico de un usuario en un item de la cola
    Optional<Vote> findByQueueItemIdAndUserId(Long queueItemId, Long userId);

    // Cuenta cuántos votos tiene un item de la cola
    int countByQueueItemId(Long queueItemId);

    // Verifica si el usuario votó algo en los últimos 3 minutos en una sala
    // @Query nos permite escribir una consulta JPQL personalizada
    // JPQL es como SQL pero usando nombres de clases Java en vez de tablas
    // v.createdAt > :since → el voto fue creado después del límite de tiempo
    // v.queueItem.room.id → navegamos las relaciones: voto → item → sala
    @Query("SELECT COUNT(v) > 0 FROM Vote v " +
           "WHERE v.user.id = :userId " +
           "AND v.queueItem.room.id = :roomId " +
           "AND v.createdAt > :since")
    boolean existsRecentVoteByUserInRoom(
            @Param("userId") Long userId,
            @Param("roomId") Long roomId,
            @Param("since") LocalDateTime since);
}