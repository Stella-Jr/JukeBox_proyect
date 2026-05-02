package com.gymstream.gymstream_api.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "El username no puede estar vacio")
        @Size(min = 3, max = 50, message = "El username debe tener entre 3 y 50 caracteres")
        String username,

        @NotBlank(message = "El password no puede estar vacio")
        @Size(min = 4, max = 15, message = "El password debe tener entre 4 y 15 caracteres")
        String password
) {
}
