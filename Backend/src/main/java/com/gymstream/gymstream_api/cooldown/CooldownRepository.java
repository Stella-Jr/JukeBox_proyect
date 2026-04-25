package com.gymstream.gymstream_api.cooldown;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.Optional;

public interface CooldownRepository extends JpaRepository<Cooldown, Long> {

    // Busca un cooldown activo para una canción específica en una sala
    // Un cooldown está activo si expires_at es mayor a la fecha actual
    // SELECT * FROM cooldowns 
    // WHERE room_id = ? AND identifier = ? AND type = ? AND expires_at > ?
    Optional<Cooldown> findByRoomIdAndIdentifierAndTypeAndExpiresAtAfter(
            Long roomId,
            String identifier,
            Cooldown.CooldownType type,
            LocalDateTime now
    );
}