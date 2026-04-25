package com.gymstream.gymstream_api.queue;

import com.gymstream.gymstream_api.cooldown.Cooldown;
import com.gymstream.gymstream_api.cooldown.CooldownRepository;
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
import java.util.ArrayList;
import java.util.Optional;
import java.util.List;


@Service
public class QueueService {

    private final QueueRepository queueRepository;
    private final VoteRepository voteRepository;
    private final SongRepository songRepository;
    private final RoomRepository roomRepository;
    private final AppUserRepository userRepository;
    private final CooldownRepository cooldownRepository;

    public QueueService(
            QueueRepository queueRepository,
            VoteRepository voteRepository,
            SongRepository songRepository,
            RoomRepository roomRepository,
            AppUserRepository userRepository,
            CooldownRepository cooldownRepository) {
        this.queueRepository = queueRepository;
        this.voteRepository = voteRepository;
        this.songRepository = songRepository;
        this.roomRepository = roomRepository;
        this.userRepository = userRepository;
        this.cooldownRepository = cooldownRepository;
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
        
        // 3.5 VALIDACIÓN DE COOLDOWN
        // Verificamos si la canción está en periodo de cooldown
        // Si alguien la intenta agregar antes de que pasen 15 minutos, la bloqueamos
        Optional<Cooldown> cooldownActivo = cooldownRepository
                .findByRoomIdAndIdentifierAndTypeAndExpiresAtAfter(
                roomId,
                song.getYoutubeId(),
                Cooldown.CooldownType.SONG,
                LocalDateTime.now()
        );

        if (cooldownActivo.isPresent()) {
            // Calculamos cuántos minutos faltan para que expire el cooldown
            long minutosRestantes = java.time.Duration.between(
                    LocalDateTime.now(),
                    cooldownActivo.get().getExpiresAt()
            ).toMinutes() + 1;

            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Esta canción no puede agregarse por " + minutosRestantes + " minutos más"
            );
        }

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
                    "Debés esperar un momento antes de votar otra canción");
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

    public List<QueueItemDTO> getQueue(Long roomId) {

        // 1. Obtenemos todas las canciones PENDING de la sala
        List<QueueItem> pendingItems = queueRepository
                .findByRoomIdAndStatus(roomId, QueueItem.QueueStatus.PENDING);

        // 2. Obtenemos la canción que está PLAYING (si hay una)
        List<QueueItem> playingItems = queueRepository
                .findByRoomIdAndStatus(roomId, QueueItem.QueueStatus.PLAYING);
 
        // 3. Calculamos el score de cada canción PENDING
        // Score = (votos × 10) + minutos en espera
        // Esto hace que canciones con pocos votos pero mucho tiempo de espera
        // también puedan subir en la cola
        List<QueueItemDTO> result = new ArrayList<>();

        // Primero detectamos el artista de la canción que está sonando
        // para aplicar la penalización anti-artista-repetido
        String currentArtist = null;
        if (!playingItems.isEmpty()) {
            QueueItem playing = playingItems.get(0);
            currentArtist = playing.getSong().getArtist();
            // La canción PLAYING siempre va primero con score máximo
            result.add(new QueueItemDTO(playing, Double.MAX_VALUE));
        }

        // Calculamos el score de cada canción pendiente
        String artistToAvoid = currentArtist; // variable final para usar en lambda
        List<QueueItemDTO> pendingDTOs = new ArrayList<>();

        for (QueueItem item : pendingItems) {
            // Calculamos los minutos que lleva esperando en la cola
            long minutosEspera = java.time.Duration.between(
                    item.getAddedAt(),
                    java.time.LocalDateTime.now()
            ).toMinutes();

            // Fórmula del score
            double score = (item.getVotesCount() * 10.0) + minutosEspera;

            // REGLA ANTI-ARTISTA-REPETIDO
            // Si el artista es el mismo que el que está sonando, penalizamos
            // restándole 1000 puntos al score (lo manda casi al final)
            if (artistToAvoid != null &&
                artistToAvoid.equalsIgnoreCase(item.getSong().getArtist())) {
                score -= 1000;
            }

            pendingDTOs.add(new QueueItemDTO(item, score));
        }

        // Ordenamos por score de mayor a menor
        pendingDTOs.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));

        // Agregamos las PENDING ordenadas después de la PLAYING
        result.addAll(pendingDTOs);

        return result;
    }

    public QueueItemDTO nextTrack(Long roomId) {

        // 1. Buscamos la canción que está sonando actualmente
        List<QueueItem> playingItems = queueRepository
                .findByRoomIdAndStatus(roomId, QueueItem.QueueStatus.PLAYING);

        // Si hay una canción sonando, la marcamos como PLAYED
        if (!playingItems.isEmpty()) {
            QueueItem currentlyPlaying = playingItems.get(0);
            currentlyPlaying.setStatus(QueueItem.QueueStatus.PLAYED);
            currentlyPlaying.setPlayedAt(LocalDateTime.now());
            queueRepository.save(currentlyPlaying);

            // Registramos el cooldown de 15 minutos para esta canción
            Cooldown cooldown = new Cooldown();
            cooldown.setRoom(currentlyPlaying.getRoom());
            cooldown.setIdentifier(currentlyPlaying.getSong().getYoutubeId());
            cooldown.setType(Cooldown.CooldownType.SONG);
            cooldown.setExpiresAt(LocalDateTime.now().plusMinutes(15));
            cooldownRepository.save(cooldown);
        }
        // 2. Obtenemos todas las canciones PENDING y calculamos sus scores
        // Reutilizamos la misma lógica del algoritmo de prioridad
        List<QueueItem> pendingItems = queueRepository
                .findByRoomIdAndStatus(roomId, QueueItem.QueueStatus.PENDING);

        if (pendingItems.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "No hay canciones en la cola");
        }

        // 3. Calculamos el score de cada canción para encontrar la mejor
        QueueItem bestItem = null;
        double bestScore = Double.NEGATIVE_INFINITY;

        // Obtenemos el artista de la canción que acabó de sonar
        // para aplicar la regla anti-artista-repetido
        String lastArtist = null;
        if (!playingItems.isEmpty()) {
            lastArtist = playingItems.get(0).getSong().getArtist();
        }

        for (QueueItem item : pendingItems) {
            long minutosEspera = java.time.Duration.between(
                    item.getAddedAt(),
                    LocalDateTime.now()
            ).toMinutes();

            double score = (item.getVotesCount() * 10.0) + minutosEspera;

            // Penalizamos si es el mismo artista que la canción anterior
            if (lastArtist != null &&
                lastArtist.equalsIgnoreCase(item.getSong().getArtist())) {
                score -= 1000;
            }

            // Guardamos el item con mejor score
            if (score > bestScore) {
                bestScore = score;
                bestItem = item;
            }
        }

        // Si no hay canciones pendientes, retornamos null
        if (bestItem == null) {
            return null;
        }

        // 4. Marcamos la siguiente canción como PLAYING
        bestItem.setStatus(QueueItem.QueueStatus.PLAYING);
        QueueItem savedItem = queueRepository.save(bestItem);

        // 5. Devolvemos los datos para que el IFrame de YouTube cargue la canción
        return new QueueItemDTO(savedItem, bestScore);
    }
}