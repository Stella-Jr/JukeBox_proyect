package com.gymstream.gymstream_api.room;

import com.gymstream.gymstream_api.user.AppUser;
import com.gymstream.gymstream_api.user.AppUserRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RoomServiceTest {

    private final RoomRepository roomRepository = mock(RoomRepository.class);
    private final AppUserRepository userRepository = mock(AppUserRepository.class);
    private final RoomService roomService = new RoomService(roomRepository, userRepository);

    @Test
    void createRoomTrimsNameAndGeneratesCode() {
        when(roomRepository.findByCode(any())).thenReturn(Optional.empty());
        when(roomRepository.save(any(Room.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Room room = roomService.createRoom("  Sala Principal  ", null);

        assertEquals("Sala Principal", room.getName());
        assertEquals(6, room.getCode().length());
    }

    @Test
    void createRoomRejectsBlankName() {
        assertThrows(IllegalArgumentException.class, () -> roomService.createRoom("  ", null));
    }
}
