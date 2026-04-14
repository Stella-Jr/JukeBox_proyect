package com.gymstream.gymstream_api.song;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "songs")
public class Song {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "youtube_id", unique = true, nullable = false, length = 20)
    private String youtubeId;

    @Column(nullable = false)
    private String title;

    @Column(length = 100)
    private String artist;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;
}