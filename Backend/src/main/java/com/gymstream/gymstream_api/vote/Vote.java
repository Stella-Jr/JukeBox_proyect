package com.gymstream.gymstream_api.vote;

import com.gymstream.gymstream_api.queue.QueueItem;
import com.gymstream.gymstream_api.user.AppUser;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "votes",
    uniqueConstraints = @UniqueConstraint(columnNames = {"queue_id", "user_id"}))
public class Vote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "queue_id")
    private QueueItem queueItem;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private AppUser user;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}