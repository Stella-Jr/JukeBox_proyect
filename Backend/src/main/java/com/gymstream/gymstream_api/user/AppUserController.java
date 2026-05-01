package com.gymstream.gymstream_api.user;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/rooms")
public class AppUserController {

    private final AppUserService userService;

    public AppUserController(AppUserService userService) {
        this.userService = userService;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest request) {
        AppUser user = userService.login(request.username(), request.password());

        return ResponseEntity.ok(Map.of(
                "token", user.getSessionToken(),
                "userId", user.getId(),
                "username", user.getUsername()
        ));
    }

    @PostMapping("/join")
    public ResponseEntity<Map<String, Object>> joinRoom(@Valid @RequestBody JoinRoomRequest request) {
        AppUser user = userService.joinRoom(request.code(), request.username());

        return ResponseEntity.ok(Map.of(
                "token", user.getSessionToken(),
                "userId", user.getId(),
                "roomId", user.getRoom().getId(),
                "roomCode", user.getRoom().getCode()
        ));
    }
}
