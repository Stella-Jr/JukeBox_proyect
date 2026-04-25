package com.gymstream.gymstream_api.queue;

import lombok.Data;

// DTO = Data Transfer Object
// No es una entidad — solo transporta datos al frontend
// Incluye el score calculado que no existe en la DB
@Data
public class QueueItemDTO {

    private Long id;
    private String songTitle;
    private String songArtist;
    private String songYtId;
    private String songThumb;
    private Integer votesCount;
    private String status;
    private Double score; // el score calculado por el algoritmo

    // Constructor que convierte un QueueItem a este DTO
    // y calcula el score automáticamente
    public QueueItemDTO(QueueItem item, double score) {
        this.id = item.getId();
        this.songTitle = item.getSong().getTitle();
        this.songArtist = item.getSong().getArtist();
        this.songYtId = item.getSong().getYoutubeId();
        this.songThumb = item.getSong().getThumbnailUrl();
        this.votesCount = item.getVotesCount();
        this.status = item.getStatus().name();
        this.score = score;
    }
}