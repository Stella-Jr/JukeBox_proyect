package com.gymstream.gymstream_api.room;

import org.springframework.stereotype.Service;
import java.util.UUID;

@Service  
public class RoomService {

    private final RoomRepository roomRepository;

    public RoomService(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    public Room createRoom(String name) {
        Room room = new Room();
        room.setName(name);
        room.setCode(generateCode()); 
        return roomRepository.save(room);
    }

    public Room getRoomByCode(String code) {
        return roomRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Sala no encontrada con código: " + code));
    }

    private String generateCode() {
        return UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 6)
                .toUpperCase();
    }
}