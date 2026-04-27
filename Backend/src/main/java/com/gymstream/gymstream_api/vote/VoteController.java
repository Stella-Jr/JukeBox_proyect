package com.gymstream.gymstream_api.vote;

import com.gymstream.gymstream_api.queue.QueueItem;
import com.gymstream.gymstream_api.queue.QueueService;
import com.gymstream.gymstream_api.realtime.RealtimeNotifier;
import com.gymstream.gymstream_api.user.AppUser;
import com.gymstream.gymstream_api.user.AppUserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/votes")
public class VoteController {

    private final QueueService queueService;
    private final AppUserService userService;
    private final RealtimeNotifier realtimeNotifier;

    public VoteController(
            QueueService queueService,
            AppUserService userService,
            RealtimeNotifier realtimeNotifier) {
        this.queueService = queueService;
        this.userService = userService;
        this.realtimeNotifier = realtimeNotifier;
    }

    @PostMapping("/{queueItemId}")
    public ResponseEntity<Map<String, Object>> vote(
            @PathVariable Long queueItemId,
            @RequestHeader("X-Session-Token") String sessionToken) {

        AppUser user = userService.getUserBySessionToken(sessionToken);
        QueueItem item = queueService.vote(queueItemId, user);
        notifyQueue(item.getRoom().getId());

        return ResponseEntity.ok(Map.of(
                "newScore", item.getVotesCount(),
                "queueId", item.getId()
        ));
    }

    @DeleteMapping("/{queueItemId}")
    public ResponseEntity<Map<String, Object>> unvote(
            @PathVariable Long queueItemId,
            @RequestHeader("X-Session-Token") String sessionToken) {

        AppUser user = userService.getUserBySessionToken(sessionToken);
        QueueItem item = queueService.unvote(queueItemId, user);
        notifyQueue(item.getRoom().getId());

        return ResponseEntity.ok(Map.of(
                "newScore", item.getVotesCount(),
                "queueId", item.getId()
        ));
    }

    private void notifyQueue(Long roomId) {
        realtimeNotifier.notifyQueueRefreshed(roomId, queueService.getQueue(roomId));
    }
}
