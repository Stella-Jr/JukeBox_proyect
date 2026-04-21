package com.gymstream.gymstream_api.user;

import com.gymstream.gymstream_api.room.Room;
import com.gymstream.gymstream_api.room.RoomService;
import org.springframework.stereotype.Service;
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
        Room room = roomService.getRoomByCode(roomCode);

        AppUser user = new AppUser();
        user.setUsername(username);
        user.setRoom(room);
        user.setSessionToken(UUID.randomUUID().toString());

        return userRepository.save(user);
    }
}