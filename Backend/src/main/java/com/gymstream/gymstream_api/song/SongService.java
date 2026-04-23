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

    private final RestClient restClient = RestClient.create();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String YOUTUBE_SEARCH_URL = "https://www.googleapis.com/youtube/v3/search";
    private static final String YOUTUBE_VIDEOS_URL = "https://www.googleapis.com/youtube/v3/videos";

    public List<SongSearchResult> searchSongs(String query) {

        // Detectamos si el texto que llegó es un link de YouTube
        // Si es un link, extraemos el videoId y buscamos ese video específico
        // Si no es un link, buscamos normalmente por texto
        String videoId = extractVideoId(query);

        if (videoId != null) {
            // ES UN LINK → buscamos el video específico por ID
            return searchByVideoId(videoId);
        } else {
            // NO ES UN LINK → búsqueda normal por texto
            return searchByQuery(query);
        }
    }

    // Detecta si el texto es un link de YouTube y extrae el videoId
    // Soporta estos formatos:
    // - https://www.youtube.com/watch?v=yKNxeF4KMsY
    // - https://youtu.be/yKNxeF4KMsY
    // - https://www.youtube.com/watch?v=yKNxeF4KMsY&t=30s (con parámetros extra)
    // Devuelve null si no es un link de YouTube
    private String extractVideoId(String text) {

        if (text == null || text.trim().isEmpty()) {
            return null;
        }

        // Formato largo: youtube.com/watch?v=ID
        if (text.contains("youtube.com/watch")) {
            // Buscamos el parámetro "v=" en la URL
            int vIndex = text.indexOf("v=");
            if (vIndex != -1) {
                // Tomamos todo lo que viene después de "v="
                String afterV = text.substring(vIndex + 2);
                // El ID termina en "&" si hay más parámetros, o en el final del string
                // Por ejemplo: "yKNxeF4KMsY&t=30s" → tomamos solo "yKNxeF4KMsY"
                int ampIndex = afterV.indexOf("&");
                if (ampIndex != -1) {
                    return afterV.substring(0, ampIndex);
                }
                return afterV;
            }
        }

        // Formato corto: youtu.be/ID
        if (text.contains("youtu.be/")) {
            int slashIndex = text.indexOf("youtu.be/") + 9;
            String afterSlash = text.substring(slashIndex);
            // El ID puede tener parámetros después: "yKNxeF4KMsY?t=30"
            int queryIndex = afterSlash.indexOf("?");
            if (queryIndex != -1) {
                return afterSlash.substring(0, queryIndex);
            }
            return afterSlash;
        }

        // No es un link de YouTube
        return null;
    }

    // Busca un video específico por su ID usando el endpoint /videos de YouTube
    // Esto se usa cuando el usuario pega un link directamente
    private List<SongSearchResult> searchByVideoId(String videoId) {

        List<SongSearchResult> results = new ArrayList<>();

        try {
            String responseBody = restClient.get()
                    .uri(YOUTUBE_VIDEOS_URL +
                            "?part=snippet" +
                            "&id=" + videoId +
                            "&key=" + apiKey)
                    .retrieve()
                    .body(String.class);

            JsonNode response = objectMapper.readTree(responseBody);

            if (response != null && response.has("items") && 
                response.get("items").size() > 0) {

                JsonNode item = response.get("items").get(0);
                JsonNode snippet = item.get("snippet");

                String title = snippet.get("title").asText();
                String channelTitle = snippet.get("channelTitle").asText();
                String thumbnail = snippet
                        .get("thumbnails")
                        .get("medium")
                        .get("url")
                        .asText();

                // Solo devolvemos ese video específico
                results.add(new SongSearchResult(videoId, title, channelTitle, thumbnail));
            }

        } catch (Exception e) {
            System.err.println("Error al buscar video por ID: " + e.getMessage());
        }

        return results;
    }

    // Búsqueda normal por texto — igual que antes
    private List<SongSearchResult> searchByQuery(String query) {

        List<SongSearchResult> results = new ArrayList<>();

        try {
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

            JsonNode response = objectMapper.readTree(responseBody);

            if (response != null && response.has("items")) {
                response.get("items").forEach(item -> {
                    try {
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
                    } catch (Exception e) {
                        System.err.println("Error al procesar item: " + e.getMessage());
                    }
                });
            }

        } catch (Exception e) {
            System.err.println("Error al buscar canciones: " + e.getMessage());
        }

        return results;
    }
}