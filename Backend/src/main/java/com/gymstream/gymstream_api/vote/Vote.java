package com.gymstream.gymstream_api.vote;

import com.gymstream.gymstream_api.queue.QueueItem;
import com.gymstream.gymstream_api.user.AppUser;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;

// Restricción UNIQUE(queue_id, user_id): Un usuario solo puede votar UNA VEZ por canción
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

    // Usar @CreationTimestamp - Hibernate lo captura automáticamente
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @Column(name = "vote_type")
    private Integer voteType = 1;

    // Constructor vacío requerido por Hibernate
    public Vote() {
    }
}