package com.gymstream.gymstream_api.cooldown;

import com.gymstream.gymstream_api.room.Room;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "cooldowns")
public class Cooldown {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "room_id")
    private Room room;

    @Column(length = 100)
    private String identifier;

    @Enumerated(EnumType.STRING)
    private CooldownType type;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    // SONG: Bloquea agregar la misma canción (15 minutos)
    // ARTIST: Bloquea agregar canciones del mismo artista (futuro)
    public enum CooldownType {
        SONG, ARTIST
    }
}