package com.gymstream.gymstream_api.room;

import jakarta.validation.Valid;
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

    @PostMapping
    public ResponseEntity<Map<String, Object>> createRoom(@Valid @RequestBody CreateRoomRequest request) {
        Room room = roomService.createRoom(request.name());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(Map.of(
                        "id", room.getId(),
                        "code", room.getCode()
                ));
    }

    @GetMapping("/{code}")
    public ResponseEntity<Room> getRoomByCode(@PathVariable String code) {
        Room room = roomService.getRoomByCode(code);
        return ResponseEntity.ok(room);
    }
}
