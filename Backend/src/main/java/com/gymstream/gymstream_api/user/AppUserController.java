package com.gymstream.gymstream_api.user;

import jakarta.validation.constraints.NotBlank;
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

    @PostMapping("/join")
    // Validar que el código de sala y nombre de usuario no estén vacíos
    public ResponseEntity<Map<String, Object>> joinRoom(@RequestBody Map<String, String> body) {
        String code = body.get("code");
        String username = body.get("username");

        // Validar que los parámetros requeridos estén presentes
        if (code == null || code.isEmpty()) {
            throw new IllegalArgumentException("El código de sala es requerido");
        }
        if (username == null || username.isEmpty()) {
            throw new IllegalArgumentException("El nombre de usuario es requerido");
        }

        AppUser user = userService.joinRoom(code, username);

        return ResponseEntity.ok(Map.of(
                "token", user.getSessionToken(),
                "userId", user.getId(),
                "roomId", user.getRoom().getId()
        ));
    }
}