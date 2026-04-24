package com.gymstream.gymstream_api.room;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController       
@RequestMapping("/api/rooms")  
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    // POST /api/rooms
    // Recibe: { "name": "Sala Principal" }
    // Devuelve: { "id": 1, "code": "A3F9K2" }
    @PostMapping
    // Validar que el request body tenga datos válidos
    public ResponseEntity<Map<String, Object>> createRoom(@RequestBody Map<String, String> body) {
        String name = body.get("name");
        
        // Validar que el nombre esté presente
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("El nombre de la sala es requerido");
        }
        
        Room room = roomService.createRoom(name);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(Map.of(
                        "id", room.getId(),
                        "code", room.getCode()
                ));
    }

    // GET /api/rooms/{code}
    // Devuelve los datos de una sala por su código
    @GetMapping("/{code}")
    public ResponseEntity<Room> getRoomByCode(@PathVariable String code) {
        Room room = roomService.getRoomByCode(code);
        return ResponseEntity.ok(room); 
    }
}