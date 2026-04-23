package com.gymstream.gymstream_api.song;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;

@Service
public class SongService {

    @Value("${youtube.api.key}")
    private String apiKey;

    private final RestClient restClient = RestClient.create();

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String YOUTUBE_SEARCH_URL = "https://www.googleapis.com/youtube/v3/search";

    public List<SongSearchResult> searchSongs(String query) {
        // Validar que la búsqueda no sea nula o vacía
        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("El término de búsqueda no puede estar vacío");
        }

        List<SongSearchResult> results = new ArrayList<>();

        try {
            // Validar que la clave API esté configurada
            if (apiKey == null || apiKey.isEmpty()) {
                throw new RuntimeException("Clave de API de YouTube no configurada");
            }

            // Realizar solicitud a YouTube API con manejo de errores
            String responseBody = restClient.get()
                    .uri(YOUTUBE_SEARCH_URL +
                            "?part=snippet" +
                            "&q=" + query +
                            "&type=video" +
                            "&videoEmbeddable=true" +
                            "&maxResults=8" +
                            "&key=" + apiKey)
                    .retrieve()
                    .body(String.class);

            // Validar que la respuesta no sea nula
            if (responseBody == null || responseBody.isEmpty()) {
                throw new RuntimeException("Respuesta vacía de YouTube API");
            }

            // Parsear respuesta JSON
            JsonNode response = objectMapper.readTree(responseBody);

            // Validar que exista el array de items
            if (response != null && response.has("items")) {
                response.get("items").forEach(item -> {
                    try {
                        // Validar que existan los campos requeridos
                        if (item.has("id") && item.get("id").has("videoId") && 
                            item.has("snippet")) {
                            
                            String videoId = item.get("id").get("videoId").asText();
                            JsonNode snippet = item.get("snippet");
                            String title = snippet.get("title").asText();
                            String channelTitle = snippet.get("channelTitle").asText();
                            String thumbnail = snippet.get("thumbnails").get("medium").get("url").asText();

                            // Validar que los campos obligatorios no estén vacíos
                            if (!videoId.isEmpty() && !title.isEmpty()) {
                                results.add(new SongSearchResult(videoId, title, channelTitle, thumbnail));
                            }
                        }
                    } catch (Exception e) {
                        // Ignorar items mal formados, continuar procesando
                        System.err.println("Advertencia al procesar item de YouTube: " + e.getMessage());
                    }
                });
            }
        } catch (RestClientException e) {
            // Manejar errores de conectividad con YouTube API
            throw new RuntimeException("Error al conectar con YouTube API: " + e.getMessage(), e);
        } catch (Exception e) {
            // Manejar otros errores de procesamiento
            throw new RuntimeException("Error al procesar respuesta de YouTube: " + e.getMessage(), e);
        }

        return results;
    }
}