package com.gymstream.gymstream_api.user;

import com.gymstream.gymstream_api.room.Room;
import com.gymstream.gymstream_api.room.RoomService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.util.UUID;

@Service
public class AppUserService {

    private final AppUserRepository userRepository;
    private final RoomService roomService;

    public AppUserService(AppUserRepository userRepository, RoomService roomService) {
        this.userRepository = userRepository;
        this.roomService = roomService;
    }

    public AppUser joinRoom(String roomCode, String username) {
        // Validar que el código de sala no sea nulo o vacío
        if (roomCode == null || roomCode.trim().isEmpty()) {
            throw new IllegalArgumentException("El código de la sala no puede estar vacío");
        }
        
        // Validar que el nombre de usuario no sea nulo o vacío
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre de usuario no puede estar vacío");
        }
        
        // Validar longitud del nombre de usuario
        if (username.length() > 50) {
            throw new IllegalArgumentException("El nombre de usuario no puede exceder 50 caracteres");
        }
        
        // Obtener la sala validada (lanza excepción si no existe)
        Room room = roomService.getRoomByCode(roomCode.trim());
        
        // Validar que la sala esté activa
        if (!Boolean.TRUE.equals(room.getIsActive())) {
            throw new RuntimeException("La sala no está activa");
        }

        // Crear nuevo usuario con token único de sesión
        // El sessionToken se usa para identificar la sesión del usuario en el cliente
        AppUser user = new AppUser();
        user.setUsername(username.trim());
        user.setRoom(room);
        user.setSessionToken(UUID.randomUUID().toString());

        return userRepository.save(user);
    }

    public AppUser getUserBySessionToken(String sessionToken) {
        if (sessionToken == null || sessionToken.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token de sesion requerido");
        }

        return userRepository.findBySessionToken(sessionToken.trim())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Token de sesion invalido"));
    }
}
