package com.gymstream.gymstream_api.queue;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/queue")
public class QueueController {

    private final QueueService queueService;

    public QueueController(QueueService queueService) {
        this.queueService = queueService;
    }

    // POST /api/queue/add
    // Recibe: { "ytId": "...", "title": "...", "artist": "...", "roomId": 1, "userId": 1 }
    // Devuelve: 201 Created con el QueueItem completo
    @PostMapping("/add")
    public ResponseEntity<QueueItem> addToQueue(@RequestBody Map<String, Object> body) {

        String ytId = (String) body.get("ytId");
        String title = (String) body.get("title");
        String artist = (String) body.get("artist");

        // Los números en JSON llegan como Integer, los convertimos a Long
        Long roomId = ((Number) body.get("roomId")).longValue();
        Long userId = ((Number) body.get("userId")).longValue();

        QueueItem item = queueService.addToQueue(ytId, title, artist, roomId, userId);

        return ResponseEntity.status(HttpStatus.CREATED).body(item);
    }

    // POST /api/queue/vote/{queueId}
    // Registra un voto en una canción de la cola
    // @PathVariable captura el {queueId} de la URL
    // Recibe: { "userId": 1 }
    // Devuelve: 200 OK con el QueueItem actualizado y su nuevo score
    @PostMapping("/vote/{queueId}")
    public ResponseEntity<Map<String, Object>> vote(
            @PathVariable Long queueId,
            @RequestBody Map<String, Object> body) {

        Long userId = ((Number) body.get("userId")).longValue();

        QueueItem item = queueService.vote(queueId, userId);

        return ResponseEntity.ok(Map.of(
                "newScore", item.getVotesCount(),
                "queueId", item.getId()
        ));
    }

    // GET /api/queue/{roomId}
    // Devuelve la cola ordenada por score (votos + tiempo de espera)
    // La canción PLAYING siempre va primero
    @GetMapping("/{roomId}")
    public ResponseEntity<List<QueueItemDTO>> getQueue(@PathVariable Long roomId) {
        List<QueueItemDTO> queue = queueService.getQueue(roomId);
        return ResponseEntity.ok(queue);
    }    

    // PATCH /api/queue/next-track/{roomId}
    // Lo llama la PC Host cuando termina una canción
    // Marca la actual como PLAYED, busca la siguiente y la marca como PLAYING
    @PatchMapping("/next-track/{roomId}")
    public ResponseEntity<QueueItemDTO> nextTrack(@PathVariable Long roomId) {
        QueueItemDTO next = queueService.nextTrack(roomId);

        return ResponseEntity.ok(next);
    }
}