package com.gymstream.gymstream_api.realtime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
public class RealtimeNotifier {

    private final RestClient restClient;
    private final String apiKey;
    private final boolean enabled;

    public RealtimeNotifier(
            @Value("${realtime.service.url:http://localhost:3000}") String realtimeUrl,
            @Value("${realtime.api.key:}") String apiKey,
            @Value("${realtime.enabled:true}") boolean enabled) {
        this.restClient = RestClient.builder().baseUrl(realtimeUrl).build();
        this.apiKey = apiKey;
        this.enabled = enabled;
    }

    public void notifyQueueRefreshed(Long roomId, Object queue) {
        notifyRoom(roomId, "refresh_queue", Map.of("queue", queue));
    }

    private void notifyRoom(Long roomId, String event, Object payload) {
        if (!enabled || apiKey == null || apiKey.isBlank()) {
            return;
        }

        try {
            restClient.post()
                    .uri("/internal/notify")
                    .header("x-api-key", apiKey)
                    .body(Map.of(
                            "roomId", String.valueOf(roomId),
                            "event", event,
                            "payload", payload
                    ))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception ex) {
            System.err.println("No se pudo notificar al realtime-service: " + ex.getMessage());
        }
    }
}
