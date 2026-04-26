package com.gymstream.gymstream_api.queue;

import lombok.Data;

@Data
public class QueueItemDTO {

    private Long id;
    private String title;      // antes: songTitle
    private String artist;     // antes: songArtist
    private String ytId;       // antes: songYtId
    private String thumbnail;  // antes: songThumb
    private Integer votes;     // antes: votesCount
    private String status;
    private Double score;

    public QueueItemDTO(QueueItem item, double score) {
        this.id = item.getId();

        if (item.getSong() != null) {
            this.title = item.getSong().getTitle();
            this.artist = item.getSong().getArtist();
            this.ytId = item.getSong().getYoutubeId();
            this.thumbnail = item.getSong().getThumbnailUrl();
        } else {
            this.title = "Sin título";
            this.artist = "Desconocido";
            this.ytId = "";
            this.thumbnail = "";
        }

        this.votes = item.getVotesCount();
        this.status = item.getStatus() != null ? item.getStatus().name() : "UNKNOWN";
        this.score = score;
    }
}