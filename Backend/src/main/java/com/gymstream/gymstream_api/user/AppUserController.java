package com.gymstream.gymstream_api.user;

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
    public ResponseEntity<Map<String, Object>> joinRoom(@RequestBody Map<String, String> body) {
        String code = body.get("code");
        String username = body.get("username");

        AppUser user = userService.joinRoom(code, username);

        return ResponseEntity.ok(Map.of(
                "token", user.getSessionToken(),
                "userId", user.getId(),
                "roomId", user.getRoom().getId()
        ));
    }
}