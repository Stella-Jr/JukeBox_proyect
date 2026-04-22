package com.gymstream.gymstream_api.song;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;

@Service
public class SongService {

    @Value("${youtube.api.key}")
    private String apiKey;

    // RestClient para hacer requests HTTP hacia YouTube
    private final RestClient restClient = RestClient.create();

    // ObjectMapper es la clase de Jackson que convierte JSON (texto) a objetos Java
    // La creamos una sola vez porque es costosa de instanciar
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String YOUTUBE_SEARCH_URL = "https://www.googleapis.com/youtube/v3/search";

    public List<SongSearchResult> searchSongs(String query) {

        // Ahora recibimos la respuesta como String puro (texto JSON)
        // En lugar de pedirle a Spring que lo convierta automáticamente
        // lo convertimos nosotros manualmente con ObjectMapper
        String responseBody = restClient.get()
                .uri(YOUTUBE_SEARCH_URL +
                        "?part=snippet" +
                        "&q=" + query +
                        "&type=video" +
                        "&videoEmbeddable=true" +
                        "&maxResults=8" +
                        "&key=" + apiKey)
                .retrieve()
                .body(String.class); // recibimos como String

        List<SongSearchResult> results = new ArrayList<>();

        try {
            // Convertimos el String JSON a un árbol de nodos que podemos navegar
            // readTree() lee el texto y crea una estructura navegable
            JsonNode response = objectMapper.readTree(responseBody);

            if (response != null && response.has("items")) {
                response.get("items").forEach(item -> {
                    String videoId = item.get("id").get("videoId").asText();
                    JsonNode snippet = item.get("snippet");
                    String title = snippet.get("title").asText();
                    String channelTitle = snippet.get("channelTitle").asText();
                    String thumbnail = snippet
                            .get("thumbnails")
                            .get("medium")
                            .get("url")
                            .asText();

                    results.add(new SongSearchResult(videoId, title, channelTitle, thumbnail));
                });
            }
        } catch (Exception e) {
            // Si algo falla al parsear el JSON, lo registramos y devolvemos lista vacía
            System.err.println("Error al parsear respuesta de YouTube: " + e.getMessage());
        }

        return results;
    }
}