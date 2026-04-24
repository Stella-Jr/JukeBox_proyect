package com.gymstream.gymstream_api.queue;

import com.gymstream.gymstream_api.room.Room;
import com.gymstream.gymstream_api.room.RoomRepository;
import com.gymstream.gymstream_api.song.Song;
import com.gymstream.gymstream_api.song.SongRepository;
import com.gymstream.gymstream_api.user.AppUser;
import com.gymstream.gymstream_api.user.AppUserRepository;
import com.gymstream.gymstream_api.vote.Vote;
import com.gymstream.gymstream_api.vote.VoteRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class QueueService {

    private final QueueRepository queueRepository;
    private final VoteRepository voteRepository;
    private final SongRepository songRepository;
    private final RoomRepository roomRepository;
    private final AppUserRepository userRepository;

    public QueueService(
            QueueRepository queueRepository,
            VoteRepository voteRepository,
            SongRepository songRepository,
            RoomRepository roomRepository,
            AppUserRepository userRepository) {
        this.queueRepository = queueRepository;
        this.voteRepository = voteRepository;
        this.songRepository = songRepository;
        this.roomRepository = roomRepository;
        this.userRepository = userRepository;
    }

    public QueueItem addToQueue(String ytId, String title, String artist,
                                Long roomId, Long userId) {

        // 1. Buscamos la sala. Si no existe, lanzamos error 404
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Sala no encontrada"));

        // 2. Buscamos el usuario. Si no existe, lanzamos error 404
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        // 3. Buscamos si la canción ya existe en nuestra tabla songs
        Song song = songRepository.findByYoutubeId(ytId)
                .orElseGet(() -> {
                    Song newSong = new Song();
                    newSong.setYoutubeId(ytId);
                    newSong.setTitle(title);
                    newSong.setArtist(artist);
                    return songRepository.save(newSong);
                });

        // 4. LÓGICA DE DUPLICADOS
        Optional<QueueItem> existingItem = queueRepository
                .findByRoomIdAndSongIdAndStatus(roomId, song.getId(), QueueItem.QueueStatus.PENDING);

        if (existingItem.isPresent()) {
            QueueItem item = existingItem.get();

            boolean yaVoto = voteRepository
                    .findByQueueItemIdAndUserId(item.getId(), userId)
                    .isPresent();

            if (yaVoto) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT, "Ya votaste esta canción");
            }

            Vote vote = new Vote();
            vote.setQueueItem(item);
            vote.setUser(user);
            voteRepository.save(vote);

            item.setVotesCount(item.getVotesCount() + 1);
            return queueRepository.save(item);

        } else {
            QueueItem newItem = new QueueItem();
            newItem.setRoom(room);
            newItem.setSong(song);
            newItem.setAddedBy(user);
            newItem.setStatus(QueueItem.QueueStatus.PENDING);
            newItem.setVotesCount(1);
            QueueItem savedItem = queueRepository.save(newItem);

            Vote vote = new Vote();
            vote.setQueueItem(savedItem);
            vote.setUser(user);
            voteRepository.save(vote);

            return savedItem;
        }
    }

    public QueueItem vote(Long queueItemId, Long userId) {

        // 1. Buscamos el item de la cola
        QueueItem item = queueRepository.findById(queueItemId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Canción no encontrada en la cola"));

        // 2. Buscamos el usuario
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        // 3. REGLA 1 — Voto único por canción
        boolean yaVoto = voteRepository
                .findByQueueItemIdAndUserId(queueItemId, userId)
                .isPresent();

        if (yaVoto) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Ya votaste esta canción");
        }

        // 4. REGLA 2 — Límite de tiempo entre votos
        LocalDateTime tresMinutosAtras = LocalDateTime.now().minusMinutes(3);

        boolean votoReciente = voteRepository
                .existsRecentVoteByUserInRoom(userId, item.getRoom().getId(), tresMinutosAtras);
                if (votoReciente) {
        throw new ResponseStatusException(
            HttpStatus.TOO_MANY_REQUESTS,
            "Debés esperar 3 minutos entre votos");
        }

        // 5. Registramos el voto
        Vote vote = new Vote();
        vote.setQueueItem(item);
        vote.setUser(user);
        voteRepository.save(vote);

        // 6. Incrementamos el contador de votos
        item.setVotesCount(item.getVotesCount() + 1);
        return queueRepository.save(item);
    }
}