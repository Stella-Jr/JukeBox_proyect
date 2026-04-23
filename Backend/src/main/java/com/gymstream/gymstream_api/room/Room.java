package com.gymstream.gymstream_api.room;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "rooms")
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Código único de la sala (6 caracteres), obligatorio
    @NotBlank(message = "El código de la sala no puede estar vacío")
    @Size(max = 10, message = "El código no puede exceder 10 caracteres")
    @Column(unique = true, nullable = false, length = 10)
    private String code;

    // Nombre de la sala, máximo 100 caracteres
    @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
    @Column(length = 100)
    private String name;

    // Estado de la sala (activa o inactiva)
    @Column(name = "is_active")
    private Boolean isActive = true;

    // Fecha y hora de creación
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}