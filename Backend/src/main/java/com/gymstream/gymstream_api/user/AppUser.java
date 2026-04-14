package com.gymstream.gymstream_api.user;

import com.gymstream.gymstream_api.room.Room;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "users")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "room_id")
    private Room room;

    @Column(nullable = false, length = 50)
    private String username;

    @Column(name = "session_token", unique = true)
    private String sessionToken;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;
}