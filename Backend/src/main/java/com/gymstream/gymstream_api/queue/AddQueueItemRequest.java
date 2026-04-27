package com.gymstream.gymstream_api.queue;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AddQueueItemRequest(
        @NotNull(message = "roomId es requerido")
        Long roomId,

        @NotBlank(message = "ytId es requerido")
        String ytId,

        @NotBlank(message = "title es requerido")
        String title,

        String artist,

        String thumb
) {
}
