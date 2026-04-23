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
import java.util.Optional;

@Service
public class QueueService {

    // Inyectamos todos los repositorios que necesitamos
    // Spring los crea y entrega automáticamente
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
        // Si no existe, la creamos. Esto evita duplicados en la tabla songs
        Song song = songRepository.findByYoutubeId(ytId)
                .orElseGet(() -> {
                    Song newSong = new Song();
                    newSong.setYoutubeId(ytId);
                    newSong.setTitle(title);
                    newSong.setArtist(artist);
                    return songRepository.save(newSong);
                });

        // 4. LÓGICA DE DUPLICADOS
        // Verificamos si la canción ya está en la cola de esta sala con status PENDING
        Optional<QueueItem> existingItem = queueRepository
                .findByRoomIdAndSongIdAndStatus(roomId, song.getId(), QueueItem.QueueStatus.PENDING);

        if (existingItem.isPresent()) {
            // La canción YA está en la cola → solo sumamos un voto
            QueueItem item = existingItem.get();

            // Verificamos que el usuario no haya votado ya esta canción
            // Si ya votó, lanzamos error 409 (Conflict)
            boolean yaVoto = voteRepository
                    .findByQueueItemIdAndUserId(item.getId(), userId)
                    .isPresent();

            if (yaVoto) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT, "Ya votaste esta canción");
            }

            // Registramos el voto
            Vote vote = new Vote();
            vote.setQueueItem(item);
            vote.setUser(user);
            voteRepository.save(vote);

            // Incrementamos el contador de votos en el QueueItem
            item.setVotesCount(item.getVotesCount() + 1);
            return queueRepository.save(item);

        } else {
            // La canción NO está en la cola → la agregamos como nueva
            QueueItem newItem = new QueueItem();
            newItem.setRoom(room);
            newItem.setSong(song);
            newItem.setAddedBy(user);
            newItem.setStatus(QueueItem.QueueStatus.PENDING);
            newItem.setVotesCount(1); // voto inicial del creador
            QueueItem savedItem = queueRepository.save(newItem);

            // Registramos el voto inicial del usuario que agregó la canción
            Vote vote = new Vote();
            vote.setQueueItem(savedItem);
            vote.setUser(user);
            voteRepository.save(vote);

            return savedItem;
        }
    }
}