package com.gymstream.gymstream_api.song;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
@Entity
@Table(name = "songs")
public class Song {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ID único de YouTube, obligatorio y de máximo 20 caracteres
    @NotBlank(message = "El ID de YouTube no puede estar vacío")
    @Size(max = 20, message = "El ID de YouTube no puede exceder 20 caracteres")
    @Column(name = "youtube_id", unique = true, nullable = false, length = 20)
    private String youtubeId;

    // Título de la canción, obligatorio
    @NotBlank(message = "El título no puede estar vacío")
    @Column(nullable = false)
    private String title;

    // Nombre del artista, máximo 100 caracteres
    @Size(max = 100, message = "El nombre del artista no puede exceder 100 caracteres")
    @Column(length = 100)
    private String artist;

    // URL de la miniatura
    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    // Duración en segundos, debe ser positivo si está presente
    @PositiveOrZero(message = "La duración debe ser 0 o positiva")
    @Column(name = "duration_seconds")
    private Integer durationSeconds;
}