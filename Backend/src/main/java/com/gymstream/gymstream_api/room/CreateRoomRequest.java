package com.gymstream.gymstream_api.room;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateRoomRequest(
        @NotBlank(message = "El nombre de la sala es requerido")
        @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
        String name
) {
}
