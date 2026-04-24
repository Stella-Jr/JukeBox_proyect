package com.gymstream.gymstream_api.queue;

import com.gymstream.gymstream_api.room.Room;
import com.gymstream.gymstream_api.song.Song;
import com.gymstream.gymstream_api.user.AppUser;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "queue")
public class QueueItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "room_id")
    private Room room;

    @ManyToOne
    @JoinColumn(name = "song_id")
    private Song song;

    @ManyToOne
    @JoinColumn(name = "added_by_user_id")
    private AppUser addedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", columnDefinition = "VARCHAR(20)")
    private QueueStatus status = QueueStatus.PENDING;

    @Column(name = "votes_count")
    private Integer votesCount = 0;

    @Column(name = "added_at")
    private LocalDateTime addedAt = LocalDateTime.now();

    @Column(name = "played_at")
    private LocalDateTime playedAt;

    public enum QueueStatus {
    PENDING, PLAYING, PLAYED, SKIPPED;
    }
}