package com.gymstream.gymstream_api.song;

import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/songs")
public class SongController {

    private final SongService songService;

    public SongController(SongService songService) {
        this.songService = songService;
    }

    @GetMapping("/search")
    public ResponseEntity<List<SongSearchResult>> search(
            @RequestParam 
            @NotBlank(message = "El término de búsqueda no puede estar vacío")
            String q) {
        List<SongSearchResult> results = songService.searchSongs(q);
        return ResponseEntity.ok(results); 
    }
}