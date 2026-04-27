package com.gymstream.gymstream_api.room;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
public class RoomService {

    private final RoomRepository roomRepository;

    public RoomService(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    @Transactional
    public Room createRoom(String name) {
        // Validar que el nombre no sea nulo o vacío
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre de la sala no puede estar vacío");
        }
        
        // Validar longitud máxima
        if (name.length() > 100) {
            throw new IllegalArgumentException("El nombre no puede exceder 100 caracteres");
        }
        
        Room room = new Room();
        room.setName(name.trim());
        room.setCode(generateUniqueCode()); 
        return roomRepository.save(room);
    }

    public Room getRoomByCode(String code) {
        // Validar que el código no sea nulo o vacío
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("El código de la sala no puede estar vacío");
        }
        
        return roomRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Sala no encontrada con código: " + code));
    }

    // Generar código único de 6 caracteres alfanuméricos (uppercase)
    // Usado para que los usuarios puedan unirse fácilmente a una sala
    // Ejemplo: "A3F9K2"
    private String generateUniqueCode() {
        String code;
        do {
            code = generateCode();
        } while (roomRepository.findByCode(code).isPresent());
        return code;
    }

    private String generateCode() {
        return UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 6)
                .toUpperCase();
    }
}
