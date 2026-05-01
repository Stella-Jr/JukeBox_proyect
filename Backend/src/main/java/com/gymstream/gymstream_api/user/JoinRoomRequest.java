package com.gymstream.gymstream_api.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record JoinRoomRequest(
        @NotBlank(message = "El codigo de la sala no puede estar vacio")
        String code,

        @NotBlank(message = "El nombre de usuario no puede estar vacio")
        @Size(max = 50, message = "El nombre de usuario no puede exceder 50 caracteres")
        String username,

        @NotBlank(message = "El password no puede estar vacio")
        @Size(min = 4, max = 15, message = "El password no puede exceder 15 caracteres")
        String password
) {
}
